package org.apache.sling.webresource;

import java.io.InputStream;

public interface WebResourceScriptRunnerFactory {
	public WebResourceScriptRunner createRunner(String scriptCompilerName,
			InputStream globalScriptStream);
}
