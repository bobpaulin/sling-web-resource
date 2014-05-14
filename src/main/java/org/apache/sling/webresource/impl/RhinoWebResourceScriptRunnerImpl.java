package org.apache.sling.webresource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.sling.webresource.WebResourceScriptRunner;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RhinoWebResourceScriptRunnerImpl implements WebResourceScriptRunner {
	
	static class SlingRhinoFactory extends ContextFactory
    {
        @Override
        protected boolean hasFeature(Context cx, int featureIndex)
        {
            if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE) {
                return true;
            }
            return super.hasFeature(cx, featureIndex);
        }
    }

    static {
        ContextFactory.initGlobal(new SlingRhinoFactory());
    }
	
	private Scriptable rootScope;
	
	private String scriptCompilerName;
	
	private InputStream globalScriptStream;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public RhinoWebResourceScriptRunnerImpl(String scriptCompilerName, InputStream globalScriptStream)
	{
		this.scriptCompilerName = scriptCompilerName;
		this.globalScriptStream = globalScriptStream;
		loadGlobalScripts();
	}
	
	@Override
	public String evaluateScript(InputStream script, Map<String, Object> scriptVariables) {
		Context rhinoContext = getRhinoContext();
		// We can share the scope.
        Scriptable threadScope = rhinoContext.newObject(rootScope);
        threadScope.setPrototype(rootScope);

        // We want "threadScope" to be a new top-level
        // scope, so set its parent scope to null. This
        // means that any variables created by assignments
        // will be properties of "threadScope".
        threadScope.setParentScope(null);
		if(scriptVariables != null)
        {
			for(Entry<String, Object> currentEntry : scriptVariables.entrySet())
			{
				threadScope.put(currentEntry.getKey(), threadScope, Context.toObject(
                        currentEntry.getValue(), threadScope));
			}
        }
                
        
        String compiledScript = null;
		try {
			compiledScript = (String) rhinoContext.evaluateReader(
					threadScope, new InputStreamReader(script), "src", 1, null);
		} catch (IOException e) {
			log.error("Error compiling scripts for " + scriptCompilerName, e);
		}
        return compiledScript;
	}
	
	@Override
	public void evaluateScriptInRootConext(String scriptName, InputStream script) {
		Context rhinoContext = getRhinoContext();
        try {

            rhinoContext.evaluateReader(rootScope, new InputStreamReader(
            		script), scriptName, 1, null);

        } catch (IOException e) {
			log.error("Error loading script " + scriptName + " to Root Context", e);
		}
		
	}
	
	protected void loadGlobalScripts()  {
        Context rhinoContext = getRhinoContext();
        try {

            rootScope = new Global(rhinoContext);
            
            rootScope.put("logger", rootScope, Context.toObject(log, rootScope));

            rhinoContext.evaluateReader(rootScope, new InputStreamReader(
            		globalScriptStream), scriptCompilerName, 1, null);

        } catch (IOException e) {
			log.error("Error loading global scripts for " + scriptCompilerName, e);
		} 
    }
	
	/**
     * 
     * Retrieves Rhino Context and sets language and optimizations.
     * 
     * @return
     */
    public Context getRhinoContext() {
        Context result = null;
        if (Context.getCurrentContext() == null) {
            Context.enter();
        }
        result = Context.getCurrentContext();
        result.setOptimizationLevel(-1);
        result.setLanguageVersion(Context.VERSION_1_7);
        return result;
    }

}
