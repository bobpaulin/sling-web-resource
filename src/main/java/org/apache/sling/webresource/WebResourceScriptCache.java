package org.apache.sling.webresource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;

import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.exception.WebResourceCompilerNotFoundException;

/**
 * 
 * Service that interfaces to the WebResouce compilers and JCR to produce
 * compiled Web Resources.
 * 
 * @author bpaulin
 * 
 */
public interface WebResourceScriptCache {

    /**
     * 
     * Returns Cache of compiled Web Resources. If it's out of date or does not
     * yet exists the web resource is compiled and saved to path specified in
     * complier service
     * 
     * @param session
     *            JCRSession
     * @param path
     *            Path to Web Resource file
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    public InputStream getCompiledScript(Session session, String path)
            throws WebResourceCompileException, WebResourceCompilerNotFoundException;
    
    /**
     * 
     * Get consolidated Web Resources
     * 
     * @param session
     * @param webResourceGroupName
     * @return
     * @throws WebResourceCompileException
     */
    public InputStream getCompiledWebResourceGroup(Session session, String webResourceGroupName)
            throws WebResourceCompileException;

    /**
     * 
     * Returns the Path to the compiled web resource.
     * 
     * @param session
     * @param path
     * @return
     * @throws WebResourceCompileException
     */
    public String getCompiledScriptPath(Session session, String path)
            throws WebResourceCompileException, WebResourceCompilerNotFoundException;
    /**
     * 
     * Returns the first web resource compiler that can compile a given file
     * extension.
     * 
     * @param extention
     * @return
     * @throws WebResourceCompileException
     */
    public WebResourceScriptCompiler getWebResourceCompilerForNode(
            Node sourceNode) throws WebResourceCompileException, WebResourceCompilerNotFoundException;

}
