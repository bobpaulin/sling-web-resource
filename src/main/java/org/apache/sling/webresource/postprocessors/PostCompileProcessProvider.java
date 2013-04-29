package org.apache.sling.webresource.postprocessors;

import javax.jcr.Node;
import java.io.InputStream;

public interface PostCompileProcessProvider {

    public InputStream applyPostCompileProcesses(Node sourceNode, InputStream compiledSource);
    
}
