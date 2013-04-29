package org.apache.sling.webresource;

import javax.jcr.Node;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.exception.WebResourceCompilerNotFoundException;

public interface WebResourceScriptCompilerProvider {
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
            Node sourceNode) throws WebResourceCompilerNotFoundException;
}
