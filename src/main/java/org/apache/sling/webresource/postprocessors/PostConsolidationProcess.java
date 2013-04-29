package org.apache.sling.webresource.postprocessors;

import java.io.InputStream;

public interface PostConsolidationProcess {
    
    public InputStream processCompiledStream(InputStream compiledSource);
    
    public boolean shouldProcess(String path);

}
