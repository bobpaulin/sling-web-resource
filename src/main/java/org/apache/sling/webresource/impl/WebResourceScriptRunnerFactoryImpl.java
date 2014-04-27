package org.apache.sling.webresource.impl;

import java.io.InputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.webresource.WebResourceScriptRunner;
import org.apache.sling.webresource.WebResourceScriptRunnerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component(immediate=true, metatype=true)
@Service
public class WebResourceScriptRunnerFactoryImpl implements WebResourceScriptRunnerFactory {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public WebResourceScriptRunner createRunner(String scriptCompilerName, InputStream globalScriptStream) {
    	WebResourceScriptRunner result = new RhinoWebResourceScriptRunnerImpl(scriptCompilerName, globalScriptStream);
    	log.debug("Created Rhino Script Runner");
    	return result;
    }

}
