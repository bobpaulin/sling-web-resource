package org.apache.sling.webresource.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.NodeIterator;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;
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
            Node webResourceGroupNode, WebResourceScriptCompiler compiler,
            Map<String, Object> compileOptions)
            throws WebResourceCompileException {

        Node result = null;
        try {

            InputStream compiledStream = compiler.compile(
                    JCRUtils.getFileNodeAsStream(sourceNode), compileOptions);

            String destinationPath = getCachedCompiledScriptPath(sourceNode,
                    webResourceGroupNode, compiler);

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

        try {
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
            Binary compiledBinary = valueFactory.createBinary(result);

            compiledContent.setProperty(Property.JCR_DATA, compiledBinary);
            Calendar lastModified = Calendar.getInstance();
            compiledContent.setProperty(Property.JCR_LAST_MODIFIED,
                    lastModified);

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

    /**
     * 
     * Constructs that path to a consolidated web resource group.
     * 
     * @param cachePath
     * @param webResourceGroupName
     * @param webResourceExtension
     * @return
     */

    public String getWebResourceGroupPath(String cachePath,
            String webResourceGroupName, String webResourceExtension) {
        return cachePath + "/" + webResourceGroupName + "."
                + webResourceExtension;
    }

    public List<String> getCompiledWebResourceGroupPaths(Session session,
            String webResourceGroupName, boolean consolidate)
            throws WebResourceCompileException {
        Map<String, InputStream> compiledWebResourceStreamMap = new HashMap<String, InputStream>();
        Map<String, Object> compileOptions = new HashMap<String, Object>();
        List<String> result = new ArrayList<String>();
        Set<String> cachePaths = new HashSet<String>();
        try {

            Query query = session
                    .getWorkspace()
                    .getQueryManager()
                    .createQuery(
                            "SELECT * FROM [nt:file] INNER JOIN [webresource:WebResourceGroup] as webResourceGroupSet ON ISDESCENDANTNODE([nt:file], webResourceGroupSet) WHERE webResourceGroupSet.[webresource:name] = $webResourceName",
                            Query.JCR_SQL2);

            query.bindValue("webResourceName", session.getValueFactory()
                    .createValue(webResourceGroupName));

            QueryResult queryResult = query.execute();

            RowIterator resultList = queryResult.getRows();

            while (resultList.hasNext()) {
                Row currentRow = resultList.nextRow();
                Node currentResult = currentRow.getNode("nt:file");
                Node currentWebResource = currentRow
                        .getNode("webResourceGroupSet");

                try {

                    processCompileOptions(compileOptions, currentWebResource);

                    Node currentCompiledScript = getCompiledScriptNode(session,
                            currentResult, currentWebResource, compileOptions);

                    String compiledScriptPath = null;

                    String currentExtension = JCRUtils
                            .getNodeExtension(currentCompiledScript);

                    if (consolidate) {
                        compiledScriptPath = getWebResourceGroupPath(
                                WEB_RESOURCE_GROUP_CACHE_PATH,
                                webResourceGroupName, currentExtension);
                    } else {
                        compiledScriptPath = currentCompiledScript.getPath();
                    }

                    InputStream compiledStream = JCRUtils
                            .getFileNodeAsStream(currentCompiledScript);

                    InputStream consolidatedStream = compiledWebResourceStreamMap
                            .get(currentExtension);

                    if (consolidatedStream == null) {
                        consolidatedStream = compiledStream;
                    } else {
                        consolidatedStream = new SequenceInputStream(
                                consolidatedStream, compiledStream);
                    }
                    compiledWebResourceStreamMap.put(compiledScriptPath,
                            consolidatedStream);
                } catch (WebResourceCompilerNotFoundException e) {

                    log.info("Compiler Not Found for Node at Path: "
                            + currentResult.getPath());
                }
            }

            if (!session.nodeExists(WEB_RESOURCE_GROUP_CACHE_PATH)) {
                JCRUtils.createNode(session.getRootNode(),
                        WEB_RESOURCE_GROUP_CACHE_PATH);
            }

            if (!compiledWebResourceStreamMap.isEmpty()) {
                for (String currentScriptPath : compiledWebResourceStreamMap
                        .keySet()) {
                    InputStream currentStream = compiledWebResourceStreamMap
                            .get(currentScriptPath);
                    createWebResourceNode(currentScriptPath, currentStream);
                    result.add(currentScriptPath);
                }

            }

        } catch (RepositoryException e) {
            throw new WebResourceCompileException(
                    "Error consolidating Web Resource Group", e);
        }

        return result;
    }

    /**
     * 
     * Reads compile options from Node and stores in a map.
     * 
     * @param compileOptions
     * @param currentWebResource
     */
    protected void processCompileOptions(Map<String, Object> compileOptions,
            Node currentWebResource) throws RepositoryException {
        if (currentWebResource.hasNode("compileOptions")) {

            Node compileOptionsNode = currentWebResource
                    .getNode("compileOptions");

            NodeIterator compileOptionIt = compileOptionsNode.getNodes();

            while (compileOptionIt.hasNext()) {
                Node currentOption = compileOptionIt.nextNode();
                String compilerName = currentOption.getProperty(
                        "webresource:compiler").getString();
                String optionName = currentOption.getProperty(
                        "webresource:compileOptionName").getString();
                String optionValue = currentOption.getProperty(
                        "webresource:compileOptionValue").getString();
                Map<String, Object> currentCompilerOptions = (Map<String, Object>) compileOptions
                        .get(compilerName);
                if (currentCompilerOptions == null) {
                    currentCompilerOptions = new HashMap<String, Object>();
                    compileOptions.put(compilerName, currentCompilerOptions);
                }

                currentCompilerOptions.put(optionName, optionValue);
            }

        }
    }

    public String getCompiledScriptPath(Session session, String path)
            throws WebResourceCompileException,
            WebResourceCompilerNotFoundException {
        String result = null;
        try {
            Node sourceNode = session.getNode(path);
            Node compiledNode = getCompiledScriptNode(session, sourceNode, null, null);
            result = compiledNode.getPath();
        } catch (RepositoryException e) {
            throw new WebResourceCompileException(
                    "Web Resource Source not Found", e);
        }
        return result;
    }

    public Node getCompiledScriptNode(Session session, Node sourceNode,
            Node webResourceGroupNode, Map<String, Object> compileOptions)
            throws WebResourceCompileException,
            WebResourceCompilerNotFoundException {
        Node result = null;

        WebResourceScriptCompiler compiler = getWebResourceCompilerForNode(sourceNode);

        try {
            String cachedCompiledScriptPath = getCachedCompiledScriptPath(
                    sourceNode, webResourceGroupNode, compiler);

            if (session.nodeExists(cachedCompiledScriptPath)) {
                Node compiledScriptNode = session
                        .getNode(cachedCompiledScriptPath);
                Node compiledScriptContent = compiledScriptNode
                        .getNode(Property.JCR_CONTENT);
                Node webResourceScriptContent = sourceNode
                        .getNode(Property.JCR_CONTENT);
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
                        webResourceGroupNode, compiler, compileOptions);
            }
        } catch (Exception e) {
            throw new WebResourceCompileException(e);
        }

        return result;
    }

    protected String getCachedCompiledScriptPath(Node sourceNode,
            Node webResourceGroupNode, WebResourceScriptCompiler compiler)
            throws RepositoryException {
        String cachedCompiledScriptPath = null;
        // If the web resource has a custom cache path then override compiler
        // default.
        if (webResourceGroupNode != null
                && webResourceGroupNode.hasProperty("webresource:cachePath")) {
            String cacheRootPath = webResourceGroupNode.getProperty(
                    "webresource:cachePath").getString();
            String webResourcePath = webResourceGroupNode.getPath();

            String relativePath = JCRUtils.convertPathToRelative(
                    webResourcePath,
                    JCRUtils.convertNodeExtensionPath(sourceNode,
                            compiler.compiledScriptExtension()));
            cachedCompiledScriptPath = cacheRootPath + relativePath;

        } else {
            String relativePath = JCRUtils.convertPathToRelative(
                    "/",
                    JCRUtils.convertNodeExtensionPath(sourceNode,
                            compiler.compiledScriptExtension()));
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

            compiler = getWebResourceCompilerForNode(sourceNode);
        } catch (RepositoryException e) {
            throw new WebResourceCompileException(e);
        }
        return compiler;
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
