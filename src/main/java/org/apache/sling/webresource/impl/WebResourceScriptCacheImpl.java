package org.apache.sling.webresource.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.List;

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

    private List<WebResourceScriptCompiler> webResourceScriptCompilerList = new ArrayList<WebResourceScriptCompiler>();

    private WebResourceScriptCompiler[] webResourceScriptCompilers;

    public void activate(final ComponentContext context) {

    }

    /**
     * 
     * Compiles and creates Compiled Web Resource from Web Resource Source path
     * 
     * @param path
     * @param compiler
     *            Selected Web Resource compiler
     * @return
     * @throws WebResourceCompileException
     */
    protected InputStream compileWebResource(String path,
            WebResourceScriptCompiler compiler)
            throws WebResourceCompileException {

        ResourceResolver resolver = null;
        InputStream result = null;
        try {
            resolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);
            Session session = resolver.adaptTo(Session.class);
            Node rootNode = session.getRootNode();

            Node webResourceScriptNode = getScriptContentNode(path, rootNode);
            result = compiler.compile(getScriptAsStream(webResourceScriptNode));

            Node compiledNode = JCRUtils.createNode(rootNode,
                    compiler.getCacheRoot() + path);

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
            throw new WebResourceCompileException(e);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }

        return result;
    }

    /**
     * 
     * Convert JCR Script Content to a String
     * 
     * @param webResourceContent
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     * @throws IOException
     * @throws ValueFormatException
     */
    protected String getScriptAsString(Node webResourceContent)
            throws PathNotFoundException, RepositoryException, IOException,
            ValueFormatException {
        Property webResourceData = webResourceContent
                .getProperty(Property.JCR_DATA);

        return IOUtils.toString(webResourceData.getBinary().getStream());
    }
    
    protected InputStream getScriptAsStream(Node webResourceContent)
            throws PathNotFoundException, RepositoryException, IOException,
            ValueFormatException {
        Property webResourceData = webResourceContent
                .getProperty(Property.JCR_DATA);

        return webResourceData.getBinary().getStream();
    }

    /**
     * 
     * Retrieve Script Content from JCR
     * 
     * @param path
     * @param rootNode
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected Node getScriptContentNode(String path, Node rootNode)
            throws PathNotFoundException, RepositoryException {
        String relativePath = JCRUtils.convertPathToRelative(path);
        Node webResourceNode = rootNode.getNode(relativePath);

        Node webResourceContent = webResourceNode.getNode(Property.JCR_CONTENT);
        return webResourceContent;
    }
    
    public InputStream getCompiledWebResourceGroup(Session session, String webResourceGroupName)
            throws WebResourceCompileException
    {
        InputStream resultSteam = null;
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
                        InputStream compiledStream = getCompiledScript(session, currentResult.getPath());
                        
                        if(resultSteam == null)
                        {
                            resultSteam = compiledStream;
                        }
                        else
                        {
                            resultSteam = new SequenceInputStream(resultSteam, compiledStream);
                        }
                    }catch(WebResourceCompilerNotFoundException e){
                        
                        System.out.println("Compiler Not Found for Node at Path: " + currentResult.getPath());
                        
                    }
                    
            }
               
        }catch(RepositoryException e)
        {
            throw new WebResourceCompileException(e); 
        }
              
        return resultSteam;
    }
    
    

    public InputStream getCompiledScript(Session session, String path)
            throws WebResourceCompileException, WebResourceCompilerNotFoundException {

        InputStream result = null;
        String relativePath = JCRUtils.convertPathToRelative(path);

        WebResourceScriptCompiler compiler = getCompilerForPath(session, path);

        String cachedCompiledScriptPath = compiler.getCacheRoot()
                + relativePath;
        try {
            if (session.nodeExists(cachedCompiledScriptPath)) {
                Node compiledScriptContent = getScriptContentNode(
                        cachedCompiledScriptPath, session.getRootNode());
                Node webResourceScriptContent = getScriptContentNode(
                        relativePath, session.getRootNode());
                Property compiledScriptLastModified = compiledScriptContent
                        .getProperty(Property.JCR_LAST_MODIFIED);
                Property webResouceScriptLastModified = webResourceScriptContent
                        .getProperty(Property.JCR_LAST_MODIFIED);

                if (compiledScriptLastModified.getDate().after(
                        webResouceScriptLastModified.getDate())) {
                    result = getScriptAsStream(compiledScriptContent);
                }
            }

            // Script is either not compiled or out of date.
            if (result == null) {
                result = compileWebResource(relativePath,
                        compiler);
            }
        } catch (Exception e) {
            throw new WebResourceCompileException(e);
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
            Node sourceNode) throws WebResourceCompileException, WebResourceCompilerNotFoundException {
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
