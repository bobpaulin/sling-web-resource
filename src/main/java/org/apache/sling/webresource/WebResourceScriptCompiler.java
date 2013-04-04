package org.apache.sling.webresource;

import java.io.InputStream;

import org.apache.sling.webresource.exception.WebResourceCompileException;
import javax.jcr.Node;

/**
 * 
 * Service to compile Web Resources.
 * 
 * @author bpaulin
 * 
 */

public interface WebResourceScriptCompiler {
    /**
     * 
     * Compile a WebResource String
     * 
     * @param webResourceScript
     * @return
     */
   /* public InputStream compile(String webResourceScript)
            throws WebResourceCompileException;*/
    /**
     * 
     * Compile a WebResource Stream
     * 
     * @param webResourceScript
     * @return
     * @throws WebResourceCompileException
     */
    public InputStream compile(InputStream webResourceScript)
            throws WebResourceCompileException;

    /**
     * 
     * The path where compiled resources will be cached
     * 
     * @return path to the cache root
     */
    public String getCacheRoot();

    /**
     * 
     * Evaluates whether a compiler can compile a given extension
     * 
     * @param extention
     * @return
     */
    public boolean canCompileNode(Node sourceNode);
    
    
    /**
     * 
     * Returns the script extension that scripts are compiled to.
     * 
     * @return
     */
    public String compiledScriptExtension();
}
