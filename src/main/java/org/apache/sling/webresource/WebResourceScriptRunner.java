package org.apache.sling.webresource;

import java.io.InputStream;
import java.util.Map;

public interface WebResourceScriptRunner {
	
	public String evaluateScript(InputStream script, Map<String, Object> scriptVariables);
	
	public void evaluateScriptInRootConext(String scriptName, InputStream script);

}
