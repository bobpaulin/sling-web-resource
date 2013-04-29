package org.apache.sling.webresource.postprocessors;

import java.io.InputStream;

import javax.jcr.Node;

public interface PostCompileProcess {
    
    public InputStream processCompiledStream(InputStream compiledSource);
    
    public boolean shouldProcess(Node sourceNode);
    
}
