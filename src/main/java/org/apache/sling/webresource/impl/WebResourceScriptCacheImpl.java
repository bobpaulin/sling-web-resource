package org.apache.sling.webresource.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.NodeIterator;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.webresource.WebResourceScriptCache;
import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.exception.WebResourceCompilerNotFoundException;
import org.apache.sling.webresource.util.JCRUtils;
import org.osgi.service.component.ComponentContext;

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
@Component(immediate = true, metatype = true)
@Service
@Reference(name = "WebResourceCompilerProvider", referenceInterface = WebResourceScriptCompiler.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class WebResourceScriptCacheImpl implements WebResourceScriptCache {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<WebResourceScriptCompiler> webResourceScriptCompilerList = new ArrayList<WebResourceScriptCompiler>();

    private WebResourceScriptCompiler[] webResourceScriptCompilers;
    
    private static final String WEB_RESOURCE_GROUP_CACHE_PATH = "/var/webresource/groups";

    public void activate(final ComponentContext context) {

    }
    
    protected Node compileWebResourceToNode(Node sourceNode,
            WebResourceScriptCompiler compiler)
            throws WebResourceCompileException {

        Node result = null;
        try {
            

            InputStream compiledStream = compiler.compile(JCRUtils.getFileNodeAsStream(sourceNode));
            
            String destinationPath = compiler.getCacheRoot() + JCRUtils.convertNodeExtensionPath(sourceNode, compiler.compiledScriptExtension());

            createWebResourceNode(destinationPath, compiledStream);
            
            Session currentSession = sourceNode.getSession();
            result = currentSession.getNode(destinationPath);
        } catch (Exception e) {
            throw new WebResourceCompileException("Error Compiling Web Resource", e);
        } 

        return result;
    }

    protected void createWebResourceNode(String destinationPath, InputStream result) throws RepositoryException, WebResourceCompileException {
        ResourceResolver resolver = null;
        
        try{
            resolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);
            Session session = resolver.adaptTo(Session.class);
            Node compiledNode = JCRUtils.createNode(session.getRootNode(),
                    destinationPath);
    
            compiledNode.setPrimaryType("nt:file");
            Node compiledContent = null;
            if (compiledNode.hasNode(Property.JCR_CONTENT)) {
                compiledContent = compiledNode.getNode(Property.JCR_CONTENT);
            } else {
                compiledContent = compiledNode.addNode(Property.JCR_CONTENT,
                        "nt:resource");
            }
    
            ValueFactory valueFactory = session.getValueFactory();
            Binary compiledBinary = valueFactory
                    .createBinary(result);
    
            compiledContent.setProperty(Property.JCR_DATA, compiledBinary);
            Calendar lastModified = Calendar.getInstance();
            compiledContent.setProperty(Property.JCR_LAST_MODIFIED,
                    lastModified);
    
            session.save();
        } catch (Exception e) {
            throw new WebResourceCompileException("Error Creating Compiled Web Resource", e);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }
    }
    
    public String getWebResourceGroupPath(String webResourceGroupName, String webResourceExtension)
    {
        return WEB_RESOURCE_GROUP_CACHE_PATH + "/" + webResourceGroupName + "." + webResourceExtension;
    }
    

    public List<String> getCompiledWebResourceGroupPaths(Session session, String webResourceGroupName)
            throws WebResourceCompileException
    {
        Map<String, InputStream> compiledWebResourceStreamMap = new HashMap<String, InputStream>();
        List<String> result = new ArrayList<String>();
        try{
            
            
            Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:file] INNER JOIN [webresource:WebResourceGroup] as webResourceGroupSet ON ISDESCENDANTNODE([nt:file], webResourceGroupSet) WHERE webResourceGroupSet.[webresource:name] = $webResourceName", Query.JCR_SQL2 );
            
            query.bindValue("webResourceName", session.getValueFactory().createValue(webResourceGroupName));
            
            QueryResult queryResult = query.execute();
            
            RowIterator resultList = queryResult.getRows();
            
            while(resultList.hasNext())
            {
                Node currentResult = resultList.nextRow().getNode("nt:file");
                
                try
                {
                    Node currentCompiledScript = getCompiledScriptNode(session, currentResult);
                    
                    String currentExtension = JCRUtils.getNodeExtension(currentCompiledScript);
                    
                    InputStream compiledStream = JCRUtils.getFileNodeAsStream(currentCompiledScript);
                    
                    InputStream consolidatedStream = compiledWebResourceStreamMap.get(currentExtension);
                    
                    if(consolidatedStream == null)
                    {
                        consolidatedStream = compiledStream;
                    }
                    else
                    {
                        consolidatedStream = new SequenceInputStream(consolidatedStream, compiledStream);
                    }
                    compiledWebResourceStreamMap.put(currentExtension, consolidatedStream);
                }catch(WebResourceCompilerNotFoundException e){
                        
                    log.info("Compiler Not Found for Node at Path: " + currentResult.getPath());
                }
            }
             
            if(!session.nodeExists(WEB_RESOURCE_GROUP_CACHE_PATH))
            {
                JCRUtils.createNode(session.getRootNode(), WEB_RESOURCE_GROUP_CACHE_PATH);
            }
            
            if(!compiledWebResourceStreamMap.isEmpty())
            {
                for(String currentExtension: compiledWebResourceStreamMap.keySet())
                {
                    String webResourcePath = getWebResourceGroupPath(webResourceGroupName, currentExtension);
                    createWebResourceNode(webResourcePath, compiledWebResourceStreamMap.get(currentExtension));
                    //Pull Newly created node
                    result.add(webResourcePath);
                }
                
            }
            
        }catch(RepositoryException e)
        {
            throw new WebResourceCompileException("Error consolidating Web Resource Group",e); 
        }
        
        return result;
    }

    public InputStream getCompiledScript(Session session, String path)
            throws WebResourceCompileException, WebResourceCompilerNotFoundException {
        InputStream result = null;
        try{
            Node sourceNode = session.getNode(path);
            result = getCompiledScript(session, sourceNode);
        }catch(RepositoryException e){
            throw new WebResourceCompileException("Web Resource Source not Found", e);
        }
        return result;
    }
    
    public Node getCompiledScriptNode(Session session, Node sourceNode)
            throws WebResourceCompileException, WebResourceCompilerNotFoundException {
        Node result = null;
        

        WebResourceScriptCompiler compiler = getWebResourceCompilerForNode(sourceNode);

        
        try {
            
            String relativePath = JCRUtils.convertPathToRelative(sourceNode.getPath());
            String cachedCompiledScriptPath = compiler.getCacheRoot()
                    + relativePath;
            if (session.nodeExists(cachedCompiledScriptPath)) {
                Node compiledScriptNode = session.getNode(cachedCompiledScriptPath);
                Node compiledScriptContent = compiledScriptNode.getNode(Property.JCR_CONTENT);
                Node webResourceScriptContent = sourceNode.getNode(Property.JCR_CONTENT);
                Property compiledScriptLastModified = compiledScriptContent
                        .getProperty(Property.JCR_LAST_MODIFIED);
                Property webResouceScriptLastModified = webResourceScriptContent
                        .getProperty(Property.JCR_LAST_MODIFIED);

                if (!compiledScriptLastModified.getDate().before(
                        webResouceScriptLastModified.getDate())) {
                    result = compiledScriptNode;
                }
            }

            // Script is either not compiled or out of date.
            if (result == null) {
                result = compileWebResourceToNode(sourceNode,
                        compiler);
            }
        } catch (Exception e) {
            throw new WebResourceCompileException(e);
        }

        return result;
    }
    
    public InputStream getCompiledScript(Session session, Node sourceNode)
            throws WebResourceCompileException, WebResourceCompilerNotFoundException {
        
        InputStream result = null;
        
        Node compiledScript = getCompiledScriptNode(session, sourceNode);
        try
        {
            result = JCRUtils.getFileNodeAsStream(compiledScript);
        }catch(RepositoryException e)
        {
            throw new WebResourceCompileException("Error Retrieving Compiled Stream", e);
        }
        
        return result;
    }

    protected WebResourceScriptCompiler getCompilerForPath(Session session, String path)
            throws WebResourceCompilerNotFoundException, WebResourceCompileException {
        
        WebResourceScriptCompiler compiler = null;
        try{
            Node sourceNode = session.getNode(path);
    
            compiler = getWebResourceCompilerForNode(sourceNode);
        }catch(RepositoryException e)
        {
            throw new WebResourceCompileException(e);
        }
        return compiler;
    }
    
    public String getCompiledScriptPath(Session session, String path) throws WebResourceCompileException, WebResourceCompilerNotFoundException {
        getCompiledScript(session, path);
        WebResourceScriptCompiler compiler = getCompilerForPath(session, path);
        return compiler.getCacheRoot() + path;
    }

    public WebResourceScriptCompiler getWebResourceCompilerForNode(
            Node sourceNode) throws WebResourceCompilerNotFoundException {
        WebResourceScriptCompiler result = null;

        WebResourceScriptCompiler[] serviceProviders = getWebResourceCompilerProviders();

        if (serviceProviders != null) {
            for (WebResourceScriptCompiler currentService : serviceProviders) {
                // Select the first service that can compile a web resource with
                // this
                // extension
                if (currentService.canCompileNode(sourceNode)) {
                    result = currentService;
                    break;
                }
            }
        }

        if (result == null) {
            throw new WebResourceCompilerNotFoundException(
                    "No Compiler Found for this Web Resource Extension");
        }

        return result;
    }

    /**
     * 
     * Bind Compiler Providers
     * 
     * @param webResourceCompilerService
     */
    protected void bindWebResourceCompilerProvider(
            WebResourceScriptCompiler webResourceCompilerService) {
        synchronized (this.webResourceScriptCompilerList) {
            this.webResourceScriptCompilerList.add(webResourceCompilerService);
            this.webResourceScriptCompilers = null;
        }
    }

    /**
     * 
     * Unbind Compiler Providers
     * 
     * @param webResourceCompilerService
     */
    protected void unbindWebResourceCompilerProvider(
            WebResourceScriptCompiler webResourceCompilerService) {
        synchronized (this.webResourceScriptCompilerList) {
            this.webResourceScriptCompilerList
                    .remove(webResourceCompilerService);
            this.webResourceScriptCompilers = null;
        }
    }

    /**
     * 
     * Return list of available compilers
     * 
     * @return
     */
    private WebResourceScriptCompiler[] getWebResourceCompilerProviders() {
        WebResourceScriptCompiler[] list = this.webResourceScriptCompilers;

        if (list == null) {
            synchronized (this.webResourceScriptCompilerList) {
                this.webResourceScriptCompilers = this.webResourceScriptCompilerList
                        .toArray(new WebResourceScriptCompiler[this.webResourceScriptCompilerList
                                .size()]);
                list = this.webResourceScriptCompilers;
            }
        }

        return list;
    }

}
