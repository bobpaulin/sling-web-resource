package org.apache.sling.webresource.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.Node;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.webresource.WebResourceInventoryManager;
import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.WebResourceScriptCompilerProvider;
import org.apache.sling.webresource.compiler.impl.AbstractNoOpCompiler;
import org.apache.sling.webresource.compiler.impl.CssNoOpCompilerImpl;
import org.apache.sling.webresource.exception.WebResourceCompilerNotFoundException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@Component(label="Web Resource Compiler Provider Service", immediate = true)
@Service
@Reference(name = "WebResourceCompilerProvider", referenceInterface = WebResourceScriptCompiler.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class WebResourceScriptCompilerProviderImpl implements
        WebResourceScriptCompilerProvider {
    
    private List<WebResourceScriptCompiler> webResourceScriptCompilerList = new ArrayList<WebResourceScriptCompiler>();

    private WebResourceScriptCompiler[] webResourceScriptCompilers;
    
    @Reference
    private EventAdmin eventAdmin;
    
    public WebResourceScriptCompiler getWebResourceCompilerForNode(
            Node sourceNode) throws WebResourceCompilerNotFoundException {
        WebResourceScriptCompiler result = null;

        WebResourceScriptCompiler[] serviceProviders = getWebResourceCompilerProviders();

        if (serviceProviders != null) {
            for (WebResourceScriptCompiler currentService : serviceProviders) {
                // Select the first service that can compile a web resource with
                // this
                // extension
                if (currentService.canCompileNode(sourceNode)) {
                    result = currentService;
                    break;
                }
            }
        }

        if (result == null) {
            throw new WebResourceCompilerNotFoundException(
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
        
        if(!(webResourceCompilerService instanceof AbstractNoOpCompiler))
        {
	        Dictionary<String, Object> properties = new Hashtable<String, Object>();
	        Event event = new Event(WebResourceInventoryManager.COMPILE_ALL_EVENT, properties);
	        this.eventAdmin.postEvent(event);
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
