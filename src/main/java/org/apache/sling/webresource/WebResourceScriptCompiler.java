package org.apache.sling.webresource;

import org.apache.sling.webresource.exception.WebResourceCompileException;

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
    public String compile(String webResourceScript) throws WebResourceCompileException;
    
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
    public boolean canCompileExtension(String extention);
}
