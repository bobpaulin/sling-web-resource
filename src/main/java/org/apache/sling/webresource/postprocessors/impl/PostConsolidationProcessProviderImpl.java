package org.apache.sling.webresource.postprocessors.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.webresource.postprocessors.PostConsolidationProcess;
import org.apache.sling.webresource.postprocessors.PostConsolidationProcessProvider;

@Component(label = "Web Resource Post Consolidation Provider Service", immediate = true)
@Service
@Reference(name = "WebResourcePostConsolidationProcessProvider", referenceInterface = PostConsolidationProcess.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class PostConsolidationProcessProviderImpl implements
		PostConsolidationProcessProvider {

	private List<PostConsolidationProcess> webResourcePostConsolidationProcessList = new ArrayList<PostConsolidationProcess>();

	private PostConsolidationProcess[] webResourcePostConsolidationProcesses;

	@Override
	public InputStream applyPostConsolidationProcesses(String path,
			InputStream compiledSource) {
		InputStream result = compiledSource;

		PostConsolidationProcess[] postConsolidationProcesses = getWebResourcePostConsolidationProcessProviders();

		if (postConsolidationProcesses != null) {
			for (PostConsolidationProcess currentProcess : postConsolidationProcesses) {
				if (currentProcess.shouldProcess(path)) {
					result = currentProcess.processCompiledStream(result);
				}
			}
		}
		return result;
	}

	protected void bindWebResourcePostConsolidationProcessProvider(
			PostConsolidationProcess webResourcePostConsolidationProcessService) {
		synchronized (this.webResourcePostConsolidationProcessList) {
			this.webResourcePostConsolidationProcessList
					.add(webResourcePostConsolidationProcessService);
			this.webResourcePostConsolidationProcesses = null;
		}
	}

	protected void unbindWebResourcePostConsolidationProcessProvider(
			PostConsolidationProcess webResourcePostConsolidationProcessService) {
		synchronized (this.webResourcePostConsolidationProcessList) {
			this.webResourcePostConsolidationProcessList
					.remove(webResourcePostConsolidationProcessService);
			this.webResourcePostConsolidationProcesses = null;
		}
	}

	private PostConsolidationProcess[] getWebResourcePostConsolidationProcessProviders() {
		PostConsolidationProcess[] list = this.webResourcePostConsolidationProcesses;

		if (list == null) {
			synchronized (this.webResourcePostConsolidationProcessList) {
				this.webResourcePostConsolidationProcesses = this.webResourcePostConsolidationProcessList
						.toArray(new PostConsolidationProcess[this.webResourcePostConsolidationProcessList
								.size()]);
				list = this.webResourcePostConsolidationProcesses;
			}
		}

		return list;
	}

}
