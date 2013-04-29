package org.apache.sling.webresource.postprocessors.impl;

import javax.jcr.Node;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.postprocessors.PostCompileProcess;
import org.apache.sling.webresource.postprocessors.PostCompileProcessProvider;

@Component(label="Web Resource Post Compiler Process Provider Service", immediate = true)
@Service
@Reference(name = "WebResourcePostCompilerProcessProvider", referenceInterface = PostCompileProcess.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class PostCompileProcessProviderImpl implements
        PostCompileProcessProvider {
    
    private List<PostCompileProcess> webResourcePostCompileProcessList = new ArrayList<PostCompileProcess>();

    private PostCompileProcess[] webResourcePostCompileProcesses;

    
    @Override
    public InputStream applyPostCompileProcesses(Node sourceNode, InputStream compiledSource) {
        
        InputStream result = compiledSource;
        
        PostCompileProcess[] postCompileProcesses = getWebResourcePostCompilerProcessProviders();
        
        if(postCompileProcesses != null)
        {
            for(PostCompileProcess currentProcess: postCompileProcesses)
            {
                if(currentProcess.shouldProcess(sourceNode))
                {
                    result = currentProcess.processCompiledStream(result);
                }
            }
        }
        return result;
    }
    
    /**
     * 
     * 
     * @param webResourcePostCompileProcessService
     */
    protected void bindWebResourcePostCompilerProcessProvider(
            PostCompileProcess webResourcePostCompileProcessService) {
        synchronized (this.webResourcePostCompileProcessList) {
            this.webResourcePostCompileProcessList.add(webResourcePostCompileProcessService);
            this.webResourcePostCompileProcesses = null;
        }
    }

    /**
     * 
     * @param webResourcePostCompileProcessService
     */
    protected void unbindWebResourcePostCompilerProcessProvider(
            PostCompileProcess webResourcePostCompileProcessService) {
        synchronized (this.webResourcePostCompileProcessList) {
            this.webResourcePostCompileProcessList
                    .remove(webResourcePostCompileProcessService);
            this.webResourcePostCompileProcesses = null;
        }
    }
    
    /**
     * 
     * Return list of available compilers
     * 
     * @return
     */
    private PostCompileProcess[] getWebResourcePostCompilerProcessProviders() {
        PostCompileProcess[] list = this.webResourcePostCompileProcesses;

        if (list == null) {
            synchronized (this.webResourcePostCompileProcessList) {
                this.webResourcePostCompileProcesses = this.webResourcePostCompileProcessList
                        .toArray(new PostCompileProcess[this.webResourcePostCompileProcessList
                                .size()]);
                list = this.webResourcePostCompileProcesses;
            }
        }

        return list;
    }

}
