package org.apache.sling.webresource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.webresource.exception.WebResourceCompileException;
/**
 * 
 * Service that interfaces to the WebResouce compilers and JCR to produce compiled Web Resources.
 * 
 * @author bpaulin
 *
 */
public interface WebResourceScriptCache {
    
    
    /**
     * 
     * Returns Cache of compiled Web Resources.
     * If it's out of date or does not yet exists 
     * the web resource is compiled and saved to path
     * specified in complier service
     * 
     * @param session JCRSession
     * @param path Path to Web Resource file
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    public String getCompiledScript(Session session, String path) throws WebResourceCompileException;
    
    /**
     * 
     * Returns the first web resource compiler 
     * that can compile a given file extension.
     * 
     * @param extention
     * @return
     * @throws WebResourceCompileException
     */
    public WebResourceScriptCompiler getWebResourceCompilerForExtention(String extention) throws WebResourceCompileException;
    

}
