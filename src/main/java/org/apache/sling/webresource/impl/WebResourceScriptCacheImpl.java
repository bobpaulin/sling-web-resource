package org.apache.sling.webresource.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
     * Compile Web Resource
     * 
     * @param source
     * @param extension
     * @return
     * @throws WebResourceCompileException
     */
    protected String compile(String source, String extension)
            throws WebResourceCompileException {
        return getWebResourceCompilerForExtention(extension).compile(source);
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
    protected String compileWebResource(String path,
            WebResourceScriptCompiler compiler)
            throws WebResourceCompileException {

        ResourceResolver resolver = null;
        String result = null;
        try {
            resolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);
            Session session = resolver.adaptTo(Session.class);
            Node rootNode = session.getRootNode();

            Node webResourceScriptNode = getScriptContentNode(path, rootNode);
            result = compiler.compile(getScriptAsString(webResourceScriptNode));

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
                    .createBinary(new ByteArrayInputStream(result
                            .getBytes("UTF-8")));

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

    public String getCompiledScript(Session session, String path)
            throws WebResourceCompileException {

        String compiledScriptString = null;
        String relativePath = JCRUtils.convertPathToRelative(path);

        // Determine extension
        int extensionPosition = path.lastIndexOf(".");
        String extension = path.substring(extensionPosition + 1);

        WebResourceScriptCompiler compiler = getWebResourceCompilerForExtention(extension);

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
                    compiledScriptString = getScriptAsString(compiledScriptContent);
                }
            }

            // Script is either not compiled or out of date.
            if (compiledScriptString == null) {
                compiledScriptString = compileWebResource(relativePath,
                        compiler);
            }
        } catch (Exception e) {
            throw new WebResourceCompileException(e);
        }

        return compiledScriptString;

    }

    public WebResourceScriptCompiler getWebResourceCompilerForExtention(
            String extention) throws WebResourceCompileException {
        WebResourceScriptCompiler result = null;

        WebResourceScriptCompiler[] serviceProviders = getWebResourceCompilerProviders();

        if (serviceProviders != null) {
            for (WebResourceScriptCompiler currentService : serviceProviders) {
                // Select the first service that can compile a web resource with
                // this
                // extension
                if (currentService.canCompileExtension(extention)) {
                    result = currentService;
                    break;
                }
            }
        }

        if (result == null) {
            throw new WebResourceCompileException(
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
