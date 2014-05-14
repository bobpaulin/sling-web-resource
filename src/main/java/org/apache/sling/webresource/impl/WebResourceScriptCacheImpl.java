package org.apache.sling.webresource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.NodeIterator;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.webresource.WebResourceInventoryManager;
import org.apache.sling.webresource.WebResourceScriptCache;
import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.WebResourceScriptCompilerProvider;
import org.apache.sling.webresource.eventhandlers.BackgroundCompilerHandler;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.exception.WebResourceCompilerNotFoundException;
import org.apache.sling.webresource.model.GlobalCompileOptions;
import org.apache.sling.webresource.model.WebResourceGroup;
import org.apache.sling.webresource.postprocessors.PostCompileProcessProvider;
import org.apache.sling.webresource.postprocessors.PostConsolidationProcessProvider;
import org.apache.sling.webresource.util.JCRUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Implementation of the Web Resource Cache
 * 
 * 
 * @author bpaulin
 * 
 */
@Component(label="Web Resource Cache Service", immediate = true)
@Service
public class WebResourceScriptCacheImpl implements WebResourceScriptCache {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Reference
    private WebResourceScriptCompilerProvider webResourceScriptCompilerProvider;
    
    @Reference
    private WebResourceInventoryManager webResourceInventoryManager;
    
    @Reference
    private PostCompileProcessProvider postCompileProcessProvider;
    
    @Reference
    private PostConsolidationProcessProvider postConsolidationProcessProvider;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String WEB_RESOURCE_GROUP_CACHE_PATH = "/var/webresource/groups";
    
    private Map<String, ReentrantLock> compileLockMap;

    public void activate(final ComponentContext context) {
        compileLockMap = new HashMap<String, ReentrantLock>();
    }

    /**
     * 
     * Compiles web resource source content node to a new node representing the
     * compiled source content
     * 
     * @param sourceNode
     * @param webResourceGroupNode
     * @param compiler
     * @param compileOptions
     * @return
     * @throws WebResourceCompileException
     */
    protected Node compileWebResourceToNode(Node sourceNode,
            WebResourceGroup webResourceGroup, WebResourceScriptCompiler compiler)
            throws WebResourceCompileException {

        Node result = null;
        try {

            Map<String, Object> compileOptions = new HashMap<String, Object>();
            GlobalCompileOptions globalCompileOptions = new GlobalCompileOptions();
            globalCompileOptions.setSourcePath(sourceNode.getPath());
            compileOptions.put("global", globalCompileOptions);
            if(webResourceGroup != null)
            {
                compileOptions.putAll(webResourceGroup.getCompileOptions());
            }

            InputStream compiledStream = compiler.compile(
                    JCRUtils.getFileNodeAsStream(sourceNode), compileOptions);

            compiledStream = postCompileProcessProvider.applyPostCompileProcesses(sourceNode, compiledStream);
            
            String destinationPath = getCachedCompiledScriptPath(sourceNode,
                    webResourceGroup, compiler);
            
            createWebResourceNode(destinationPath, compiledStream);

            Session currentSession = sourceNode.getSession();
            result = currentSession.getNode(destinationPath);
        } catch (Exception e) {
            throw new WebResourceCompileException(
                    "Error Compiling Web Resource", e);
        }

        return result;
    }

    /**
     * 
     * Helper for creating compiled web resource content node
     * 
     * @param destinationPath
     * @param result
     * @throws RepositoryException
     * @throws WebResourceCompileException
     */
    protected void createWebResourceNode(String destinationPath,
            InputStream result) throws RepositoryException,
            WebResourceCompileException {
        ResourceResolver resolver = null;
        log.info("Creating Web Resource Node at path: " + destinationPath);
        try {
            resolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);
            Session session = resolver.adaptTo(Session.class);
            JCRUtils.createFileContentNode(destinationPath, result, session);
            session.save();
        } catch (Exception e) {
            throw new WebResourceCompileException(
                    "Error Creating Compiled Web Resource", e);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }
    }
    
    
    public Map<String, List<String>> getCompiledWebResourceGroupPaths(Session session,
            String webResourceGroupName, boolean consolidate)
            throws WebResourceCompileException {
    	
    	StopWatch stopWatch = new StopWatch();
    	stopWatch.start();
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        
        WebResourceGroup webResourceGroup = null;
        
        try {

            List<String> webResourcePathList = 
            		webResourceInventoryManager.getSourceWebResources(webResourceGroupName);
            
            log.debug("Compiling: "  + webResourcePathList);
            
            webResourceGroup = new WebResourceGroup(
            		session.getNode(webResourceInventoryManager.getWebResourcePathLookup(webResourceGroupName)));

            for (String currentWebResourcePath: webResourcePathList) {
                Node currentResult = session.getNode(currentWebResourcePath);

                try {

                    Node currentCompiledScript = getCompiledScriptNode(session,
                            currentResult, webResourceGroup);

                    String compiledScriptPath = currentCompiledScript.getPath();
                    
                    String currentExtension = JCRUtils
                            .getNodeExtension(currentCompiledScript);
                    
                    List<String> extentionPathList = result.get(currentExtension);
                    if(extentionPathList == null)
                    {
                        extentionPathList = new ArrayList<String>();
                        result.put(currentExtension, extentionPathList);
                    }
                    extentionPathList.add(compiledScriptPath);
                    
                } catch (WebResourceCompilerNotFoundException e) {

                    log.info("Compiler Not Found for Node at Path: "
                            + currentResult.getPath());
                }
            }
            if(consolidate)
            {
                result = consolidateWebResources(session, webResourceGroup, result);
            }
        } catch (RepositoryException e) {
            throw new WebResourceCompileException(
                    "Error consolidating Web Resource Group", e);
        }
        stopWatch.stop();
        log.info("Compilation of Web Resource Group " + webResourceGroupName + " completed in: " + stopWatch);
        return result;
    }
    
    
    /**
     * 
     * Consolidates several web resource files into one.
     * 
     * @param session
     * @param webResourceGroup
     * @param compiledWebResourcePaths
     * @return
     * @throws RepositoryException
     * @throws WebResourceCompileException
     */
    public Map<String, List<String>> consolidateWebResources(Session session, WebResourceGroup webResourceGroup, Map<String, List<String>> compiledWebResourcePaths) throws RepositoryException, WebResourceCompileException
    {
        Map<String, List<String>> resultPaths = new HashMap<String, List<String>>();
        //Find out if there is a cached copy
        StringBuffer webResourceGroupPathBuffer = new StringBuffer();
        
        if(webResourceGroup.getCachePath() != null)
        {
            webResourceGroupPathBuffer.append(webResourceGroup.getCachePath()); 
        }
        else
        {
            webResourceGroupPathBuffer.append( WEB_RESOURCE_GROUP_CACHE_PATH);
        }
        
        webResourceGroupPathBuffer.append("/");
        webResourceGroupPathBuffer.append( webResourceGroup.getName());
        webResourceGroupPathBuffer.append(".");
        
        for(String currentExtention: compiledWebResourcePaths.keySet())
        {
            String cachedWebResourcePath = webResourceGroupPathBuffer.toString() + currentExtention;
            aquireLock(cachedWebResourcePath);
            try{
            	createConsolidatedSource(session,
                            compiledWebResourcePaths, currentExtention,
                            cachedWebResourcePath);
            }finally{
                releaseLock(cachedWebResourcePath);
            }
            
            List<String> consolidatedPathListForExtention = new ArrayList<String>();
            consolidatedPathListForExtention.add(cachedWebResourcePath);
            resultPaths.put(currentExtention, consolidatedPathListForExtention);
        }
        return resultPaths;
    }

    protected void createConsolidatedSource(Session session,
            Map<String, List<String>> compiledWebResourcePaths,
            String currentExtention, String cachedWebResourcePath) throws RepositoryException,
            WebResourceCompileException {
        //Cached copy is out of date
        InputStream consolidatedInputStream = null;
        
        for(String currentResourcePath: compiledWebResourcePaths.get(currentExtention))
        {
            Node currentCompiledNode = session.getNode(currentResourcePath);
            InputStream currentInputStream = JCRUtils.getFileNodeAsStream(currentCompiledNode);
            
            if(consolidatedInputStream == null)
            {
                consolidatedInputStream = currentInputStream;
            }
            else
            {
                consolidatedInputStream = new SequenceInputStream(consolidatedInputStream, currentInputStream);
            }
        }
        postConsolidationProcessProvider.applyPostConsolidationProcesses(cachedWebResourcePath, consolidatedInputStream);
        //Write
        createWebResourceNode(cachedWebResourcePath, consolidatedInputStream);
    }
    
    public Map<String, List<String>> getWebResourceCachedInventoryPaths(Session session, String webResourceGroupName) throws RepositoryException
    {
    	Map<String, List<String>> result = new HashMap<String, List<String>>();
    	Query query = session
                .getWorkspace()
                .getQueryManager()
                .createQuery(
                        "SELECT * FROM [webresource:WebResourceGroup] as webResourceGroupSet WHERE webResourceGroupSet.[webresource:name] = $webResourceName",
                        Query.JCR_SQL2);

        query.bindValue("webResourceName", session.getValueFactory()
                .createValue(webResourceGroupName));

        QueryResult queryResult = query.execute();
        
        NodeIterator queryIt = queryResult.getNodes();
        if(queryIt.hasNext())
        {
        	WebResourceGroup webResourceGroup = new WebResourceGroup(queryIt.nextNode());
        	
        	result.putAll(webResourceGroup.getInventory());
        	
        }
        
        return result;
        
    }

    public String getCompiledScriptPath(Session session, String path)
            throws WebResourceCompileException,
            WebResourceCompilerNotFoundException {
        String result = null;
        try {
            Node sourceNode = session.getNode(path);
            Node compiledNode = getCompiledScriptNode(session, sourceNode,
                    null);
            result = compiledNode.getPath();
        } catch (RepositoryException e) {
            throw new WebResourceCompileException(
                    "Web Resource Source not Found", e);
        }
        return result;
    }

    /**
     * 
     * Obtains Node of a compiled web resource.  If needed compiles the resource.
     * 
     * @param session
     * @param sourceNode
     * @param webResourceGroup
     * @return
     * @throws WebResourceCompileException
     * @throws WebResourceCompilerNotFoundException
     */
    public Node getCompiledScriptNode(Session session, Node sourceNode,
            WebResourceGroup webResourceGroup)
            throws WebResourceCompileException,
            WebResourceCompilerNotFoundException {
        Node result = null;
        WebResourceScriptCompiler compiler = webResourceScriptCompilerProvider.getWebResourceCompilerForNode(sourceNode);
        String cachedCompiledScriptPath = null;
        try {
            cachedCompiledScriptPath = getCachedCompiledScriptPath(
                    sourceNode, webResourceGroup, compiler);
            
            aquireLock(cachedCompiledScriptPath);
            
            if (session.nodeExists(cachedCompiledScriptPath)) {
                Node compiledScriptNode = session
                        .getNode(cachedCompiledScriptPath);
                    
                if(isCacheFresh(compiledScriptNode, sourceNode)) {
                    result = compiledScriptNode;
                }
            }
    
                // Script is either not compiled or out of date.
            if (result == null) {
                result = compileWebResourceToNode(sourceNode,
                       webResourceGroup, compiler);
            }
            
        } catch (Exception e) {
            throw new WebResourceCompileException(e);
        }finally{
            releaseLock(cachedCompiledScriptPath);
        }

        return result;
    }

    /**
     * 
     * Create locks on cached scripts to prevent overcompiling.
     * 
     * @param cachedCompiledScriptPath
     * @return
     */
    protected ReentrantLock aquireLock(String cachedCompiledScriptPath) {
        ReentrantLock pathLock;
        synchronized(this)
        {
             pathLock = compileLockMap.get(cachedCompiledScriptPath);
             if(pathLock == null)
             {
                 log.debug("Created lock for Path: "+ cachedCompiledScriptPath);
                 pathLock = new ReentrantLock();
                 compileLockMap.put(cachedCompiledScriptPath, pathLock);
                 pathLock.lock();
             }
        }
        if(!pathLock.isHeldByCurrentThread())
        {
            pathLock.lock();
            pathLock = aquireLock(cachedCompiledScriptPath);
        }
        return pathLock;
    }
    
    /**
     * 
     * Release lock on compiled scripts.
     * 
     * @param cachedCompiledScriptPath
     */
    protected void releaseLock(String cachedCompiledScriptPath)
    {
        synchronized(this)
        {
            
             ReentrantLock pathLock = compileLockMap.get(cachedCompiledScriptPath);
             if(pathLock != null)
             {
                 log.debug("Releasing lock for Path: "+ cachedCompiledScriptPath);
                 pathLock.unlock();
                 //Cleans out the compile lock map to prevent memory leak
                 //Queued threads method is an estimate. However it should be an overestimate
                 //since it may return true with cancelled threads
                 if(!pathLock.hasQueuedThreads())
                 {
                     compileLockMap.remove(cachedCompiledScriptPath);
                 }
             }
        }
    }
    /**
     * 
     * Determines if the cache is fresher than the source node. 
     * 
     * @param cacheScriptNode
     * @param sourceNode
     * @return
     * @throws RepositoryException
     */
    protected boolean isCacheFresh(Node cacheScriptNode, Node sourceNode) throws RepositoryException {
        boolean cacheFresh = false;
        
        Node cacheScriptContent = cacheScriptNode
                .getNode(Property.JCR_CONTENT);
        Node webResourceScriptContent = sourceNode
                .getNode(Property.JCR_CONTENT);
        Property cacheScriptLastModified = cacheScriptContent
                .getProperty(Property.JCR_LAST_MODIFIED);
        Property webResouceScriptLastModified = webResourceScriptContent
                .getProperty(Property.JCR_LAST_MODIFIED);

        if (!cacheScriptLastModified.getDate().before(
                webResouceScriptLastModified.getDate())) {
            cacheFresh = true;
        }
        return cacheFresh;
    }

    protected String getCachedCompiledScriptPath(Node sourceNode,
            WebResourceGroup webResourceGroup, WebResourceScriptCompiler compiler)
            throws RepositoryException {
        String cachedCompiledScriptPath = null;
        // If the web resource has a custom cache path then override compiler
        // default.
        if (webResourceGroup != null
                && webResourceGroup.getCachePath() != null) {

            String relativePath = JCRUtils.convertPathToRelative(
                    webResourceGroup.getGroupPath(),
                    JCRUtils.convertNodeExtensionPath(sourceNode,
                            compiler.compiledScriptExtension()));
            cachedCompiledScriptPath = webResourceGroup.getCachePath() + relativePath;

        } else if(webResourceGroup != null) {
            String relativePath = JCRUtils.convertPathToRelative(
                    webResourceGroup.getGroupPath(),
                    JCRUtils.convertNodeExtensionPath(sourceNode,
                            compiler.compiledScriptExtension()));
            cachedCompiledScriptPath = WEB_RESOURCE_GROUP_CACHE_PATH + relativePath;
        }else {
            String relativePath = JCRUtils.convertNodeExtensionPath(sourceNode,
                            compiler.compiledScriptExtension());
            cachedCompiledScriptPath = compiler.getCacheRoot() + relativePath;
        }
        return cachedCompiledScriptPath;
    }

    protected WebResourceScriptCompiler getCompilerForPath(Session session,
            String path) throws WebResourceCompilerNotFoundException,
            WebResourceCompileException {

        WebResourceScriptCompiler compiler = null;
        try {
            Node sourceNode = session.getNode(path);

            compiler = webResourceScriptCompilerProvider.getWebResourceCompilerForNode(sourceNode);
        } catch (RepositoryException e) {
            throw new WebResourceCompileException(e);
        }
        return compiler;
    }

    
    @Override
    public String calculateWebResourceGroupHash(Session session,
    		String webResourceGroupName) {
    	return "testDummy";
    }
    

    

}
