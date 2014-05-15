package org.apache.sling.webresource.postprocessors;

import java.io.InputStream;

public interface PostConsolidationProcessProvider {

	public InputStream applyPostConsolidationProcesses(String path,
			InputStream compiledSource);

}
