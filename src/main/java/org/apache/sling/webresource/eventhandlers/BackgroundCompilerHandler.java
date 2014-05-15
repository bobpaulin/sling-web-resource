package org.apache.sling.webresource.eventhandlers;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.webresource.WebResourceInventoryManager;
import org.apache.sling.webresource.WebResourceScriptCache;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.model.WebResourceGroup;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true, immediate = true)
public class BackgroundCompilerHandler implements EventHandler {

	@Reference
	private SlingRepository repository;

	@Reference
	private WebResourceScriptCache webResourceScriptCache;

	@Reference
	private WebResourceInventoryManager webResourceInventoryManager;

	@Reference
	private EventAdmin eventAdmin;

	private ExecutorService executorService;

	private BundleContext bundleContext;

	private final Logger log = LoggerFactory.getLogger(getClass());

	protected void activate(ComponentContext context) {
		bundleContext = context.getBundleContext();

		this.executorService = Executors.newFixedThreadPool(4);

		compilePaths(new ArrayList<String>(
				this.webResourceInventoryManager.getAllWebResourcePaths()));
		String[] compileTopics = new String[] {
				WebResourceInventoryManager.COMPILE_EVENT,
				WebResourceInventoryManager.COMPILE_ALL_EVENT };
		Dictionary<String, Object> backgroundCompilerProps = new Hashtable<String, Object>();
		backgroundCompilerProps.put(EventConstants.EVENT_TOPIC, compileTopics);
		bundleContext.registerService(EventHandler.class.getName(), this,
				backgroundCompilerProps);
		log.debug("Background Compiler Activated");
	}

	@Override
	public void handleEvent(Event event) {
		List<String> paths = null;
		if (event.getTopic().equals(
				WebResourceInventoryManager.COMPILE_ALL_EVENT)) {
			paths = new ArrayList<String>(
					this.webResourceInventoryManager.getAllWebResourcePaths());
		} else {
			paths = (List<String>) event.getProperty("paths");
		}
		compilePaths(paths);
	}

	private void compilePaths(final List<String> paths) {
		Runnable compileTask = new Runnable() {

			private List<String> pathList = paths;

			@Override
			public void run() {
				Iterator<String> pendingWebResourcePathIt = this.pathList
						.iterator();
				// Find all web resource nodes
				log.info("Checking for Web Resource Group Paths that need compilation");
				// Compile all web resource nodes

				if (pendingWebResourcePathIt.hasNext()) {
					processPendingWebRequests(pendingWebResourcePathIt);
				}
			}
		};

		this.executorService.submit(compileTask);
	}

	private void processPendingWebRequests(Iterator<String> pendingWebRequestIt) {
		Session jcrSession = null;
		log.info("Starting Background Compile");
		try {
			jcrSession = repository.loginAdministrative(null);
			if (jcrSession != null) {

				while (pendingWebRequestIt.hasNext()) {
					try {
						Node currentWebResourceGroup = jcrSession
								.getNode(pendingWebRequestIt.next());
						log.info("Compiling: "
								+ currentWebResourceGroup.getPath());

						WebResourceGroup webResourceGroup = new WebResourceGroup(
								currentWebResourceGroup);

						Map<String, List<String>> webResourceGroupPaths = webResourceScriptCache
								.getCompiledWebResourceGroupPaths(jcrSession,
										webResourceGroup.getName(), true);

						if (currentWebResourceGroup
								.hasNode(WebResourceGroup.INVENTORY)) {
							currentWebResourceGroup.getNode(
									WebResourceGroup.INVENTORY).remove();
						}

						Node inventoryNode = currentWebResourceGroup.addNode(
								WebResourceGroup.INVENTORY,
								"webresource:Inventory");

						for (Entry<String, List<String>> currentWebResourceTypeEntry : webResourceGroupPaths
								.entrySet()) {
							String[] webResourcePaths = currentWebResourceTypeEntry
									.getValue().toArray(new String[0]);
							inventoryNode.setProperty(
									currentWebResourceTypeEntry.getKey(),
									webResourcePaths);
						}
						jcrSession.save();
						pendingWebRequestIt.remove();
					} catch (InvalidQueryException e) {
						log.error("Unable to compile WebResource", e);
					} catch (RepositoryException e) {
						log.error("Unable to compile WebResource", e);
					} catch (WebResourceCompileException e) {
						log.error("Unable to compile WebResource", e);
					}
				}
			}
		} catch (RepositoryException e) {
			log.error("Unable to login as admin", e);
		} finally {
			if (jcrSession != null) {

				jcrSession.logout();
			}
		}
	}

	public void setRepository(SlingRepository repository) {
		this.repository = repository;
	}

	public void setWebResourceScriptCache(
			WebResourceScriptCache webResourceScriptCache) {
		this.webResourceScriptCache = webResourceScriptCache;
	}

}
