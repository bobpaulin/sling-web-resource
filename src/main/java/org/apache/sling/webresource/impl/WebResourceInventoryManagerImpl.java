package org.apache.sling.webresource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.webresource.WebResourceInventoryManager;
import org.apache.sling.webresource.WebResourceScriptCache;
import org.apache.sling.webresource.eventhandlers.BackgroundCompilerHandler;
import org.apache.sling.webresource.model.WebResourceGroup;
import org.apache.sling.webresource.util.JCRUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true, immediate = true)
@Service
public class WebResourceInventoryManagerImpl implements
		WebResourceInventoryManager {

	@Reference
	private SlingRepository repository;

	@Reference
	private EventAdmin eventAdmin;

	private Session adminSession;

	private BundleContext bundleContext;

	private Map<String, ServiceRegistration> webResourceServiceRegistration;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Map<String, Map<String, List<String>>> webResourceExtentionInventoryMap;

	private Map<String, String> webResourceNamePathMap;

	protected void activate(ComponentContext context) {

		bundleContext = context.getBundleContext();
		webResourceServiceRegistration = new HashMap<String, ServiceRegistration>();
		webResourceExtentionInventoryMap = new HashMap<String, Map<String, List<String>>>();
		webResourceNamePathMap = new HashMap<String, String>();

		try {
			adminSession = repository.loginAdministrative(null);

			registerWebResourceGroupFolderHandler();

			webResourceNamePathMap.putAll(getWebResources(adminSession));

			for (Entry<String, String> currentWebResourceEntry : webResourceNamePathMap
					.entrySet()) {
				String webResourceGroupName = currentWebResourceEntry.getKey();

				buildInventory(webResourceGroupName);
				registerWebResourceGroupListener(webResourceGroupName,
						currentWebResourceEntry.getValue());
			}
		} catch (RepositoryException e) {
			log.error("Could not Login to admin session", e);
		}

		registerWebResourceGroupEventHandler();
	}

	private void registerWebResourceGroupEventHandler() {
		String[] topics = { TOPIC_WEB_RESOURCE_CREATED,
				TOPIC_WEB_RESOURCE_DELETED };

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(EventConstants.EVENT_TOPIC, topics);
		bundleContext.registerService(EventHandler.class.getName(),
				new WebResourceGroupEventHandler(), props);
	}

	private void registerWebResourceGroupFolderHandler() {
		String[] allSlingResourceTopics = {
				SlingConstants.TOPIC_RESOURCE_ADDED,
				SlingConstants.TOPIC_RESOURCE_REMOVED };
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(EventConstants.EVENT_TOPIC, allSlingResourceTopics);
		bundleContext.registerService(EventHandler.class.getName(),
				new WebResourceGroupFolderHandler(), props);
	}

	

	public Map<String, String> getWebResources(Session session)
			throws RepositoryException {
		Map<String, String> result = new HashMap<String, String>();
		Query query = session
				.getWorkspace()
				.getQueryManager()
				.createQuery(
						"SELECT * FROM [webresource:WebResourceGroup] as webResourceGroupSet",
						Query.JCR_SQL2);

		QueryResult queryResult = query.execute();

		NodeIterator queryIt = queryResult.getNodes();
		while (queryIt.hasNext()) {
			Node webResourceNode = queryIt.nextNode();

			result.put(webResourceNode.getProperty(WebResourceGroup.NAME)
					.getString(), webResourceNode.getPath());

		}

		return result;

	}

	private void unregisterWebResourceGroupListener(final String webResourceName)
			throws RepositoryException {
		ServiceRegistration serviceRegistration = webResourceServiceRegistration
				.get(webResourceName);
		serviceRegistration.unregister();
	}

	private void registerWebResourceGroupListener(final String webResourceName,
			final String webResourcePath) throws RepositoryException {
		if (!webResourceServiceRegistration.containsKey(webResourceName)) {
			String[] topics = new String[] {
					SlingConstants.TOPIC_RESOURCE_ADDED,
					SlingConstants.TOPIC_RESOURCE_CHANGED,
					SlingConstants.TOPIC_RESOURCE_REMOVED };

			Dictionary<String, Object> props = new Hashtable<String, Object>();
			props.put(EventConstants.EVENT_TOPIC, topics);
			props.put(EventConstants.EVENT_FILTER, "(path=" + webResourcePath
					+ "/*)");

			this.webResourceServiceRegistration.put(webResourceName,
					bundleContext.registerService(EventHandler.class.getName(),
							new InventoryEventHandler(), props));

			log.info("Registered Inventory Event Handler for "
					+ webResourceName);
		}
	}

	/**
	 * 
	 * Runs Query for Web Resource Nodes in a given group
	 * 
	 * @param session
	 * @param webResourceGroupName
	 * @return
	 * @throws RepositoryException
	 */
	protected QueryResult getWebResourceGroupQueryResults(Session session,
			String webResourceGroupName) throws RepositoryException {
		Query query = session
				.getWorkspace()
				.getQueryManager()
				.createQuery(
						"SELECT * FROM [nt:file] INNER JOIN [webresource:WebResourceGroup] as webResourceGroupSet ON ISDESCENDANTNODE([nt:file], webResourceGroupSet) WHERE webResourceGroupSet.[webresource:name] = $webResourceName",
						Query.JCR_SQL2);

		query.bindValue("webResourceName", session.getValueFactory()
				.createValue(webResourceGroupName));

		QueryResult queryResult = query.execute();
		return queryResult;
	}

	private void buildInventory(String webResourceGroupName)
			throws RepositoryException {
		log.info("Creating Inventory for Web Resource Group: "
				+ webResourceGroupName);
		QueryResult result = getWebResourceGroupQueryResults(adminSession,
				webResourceGroupName);
		RowIterator rowIterator = result.getRows();
		while (rowIterator.hasNext()) {
			Row currentRow = rowIterator.nextRow();

			Node currentFileNode = currentRow.getNode("nt:file");
			String currentPath = currentFileNode.getPath();
			String currentExtention = JCRUtils
					.getNodeExtension(currentFileNode);
			if (!(currentExtention.equals("js") || currentExtention
					.equals("css"))) {
				updateWebResourceExtensionInventory(currentPath,
						currentFileNode);
			}
			
		}
		
		log.info("Created Invetory for Web Resource Group: " + webResourceGroupName);
		if(MapUtils.isNotEmpty(webResourceExtentionInventoryMap.get(webResourceGroupName)))
		{
			for(Entry<String, List<String>> extentionListEntry: webResourceExtentionInventoryMap.get(webResourceGroupName).entrySet())
			{
				log.info("Extension: " + extentionListEntry.getKey() + " Items: " + extentionListEntry.getValue());
				
			}
			
		}
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put("paths",
				Collections.singletonList(webResourceNamePathMap.get(webResourceGroupName)));
		org.osgi.service.event.Event compileEvent = new org.osgi.service.event.Event(
				WebResourceInventoryManager.COMPILE_EVENT,
				properties);
		eventAdmin.sendEvent(compileEvent);
		
		
	}

	private String removePathFromWebResourceExtensionInventory(String path)
			throws RepositoryException {
		String webResourceGroupName = null;
		for (Entry<String, Map<String, List<String>>> currentWebResourceExtensionEntry : webResourceExtentionInventoryMap
				.entrySet()) {
			for (Entry<String, List<String>> currentExtensionEntry : currentWebResourceExtensionEntry
					.getValue().entrySet()) {

				if (currentExtensionEntry.getValue().remove(path)) {
					webResourceGroupName = currentWebResourceExtensionEntry
							.getKey();

				}
			}
		}

		return webResourceGroupName;
	}

	public String getWebResourceGroupForNode(Node childNode)
			throws RepositoryException {

		if (childNode.isNodeType(WebResourceGroup.NODE_TYPE)) {
			return childNode.getProperty(WebResourceGroup.NAME).getString();
		}
		int depth = childNode.getDepth();
		if (depth > 0) {
			return getWebResourceGroupForNode(childNode.getParent());
		} else {
			throw new ItemNotFoundException();
		}
	}

	private void updateWebResourceExtensionInventory(String path,
			Node resourceNode) throws RepositoryException {
		String webResourceGroupName = getWebResourceGroupForNode(resourceNode);
		Map<String, List<String>> extentionInventoryMap = webResourceExtentionInventoryMap
				.get(webResourceGroupName);
		if (extentionInventoryMap == null) {
			extentionInventoryMap = new HashMap<String, List<String>>();
			webResourceExtentionInventoryMap.put(webResourceGroupName,
					extentionInventoryMap);
		}
		String extension = JCRUtils.getNodeExtension(resourceNode);
		List<String> extensionInventoryList = extentionInventoryMap
				.get(extension);
		if (extensionInventoryList == null) {
			extensionInventoryList = new ArrayList<String>();
			extentionInventoryMap.put(extension, extensionInventoryList);
		}
		extensionInventoryList.add(path);
	}

	class InventoryEventHandler implements EventHandler {

		@Override
		public void handleEvent(org.osgi.service.event.Event event) {
			String eventTopic = event.getTopic();
			String path = (String) event.getProperty("path");
			String webResourceGroupPath = null;
			try {
				if (eventTopic.equals(SlingConstants.TOPIC_RESOURCE_ADDED)
						|| eventTopic
								.equals(SlingConstants.TOPIC_RESOURCE_CHANGED)) {

					Node resourceNode = adminSession.getNode(path);
					if (!ignoreInventoryEvent(event, resourceNode)) {
						log.info("Update Inventory for Web Resource Path: "
								+ path);
						updateWebResourceExtensionInventory(path, resourceNode);
						String webResourceGroupName = getWebResourceGroupForNode(resourceNode);
						webResourceGroupPath = webResourceNamePathMap
								.get(webResourceGroupName);
					}

				} else if (eventTopic
						.equals(SlingConstants.TOPIC_RESOURCE_REMOVED)) {
					Node resourceNode = adminSession.getNode(path);
					if (!ignoreInventoryEvent(event, resourceNode)) {
						log.info("Remove Inventory for Web Resource Path: "
								+ path);
						String webResourceGroupName = removePathFromWebResourceExtensionInventory(path);
						webResourceGroupPath = webResourceNamePathMap
								.get(webResourceGroupName);
					}
				}

				if (webResourceGroupPath != null) {
					Dictionary<String, Object> properties = new Hashtable<String, Object>();
					properties.put("paths",
							Collections.singletonList(webResourceGroupPath));
					org.osgi.service.event.Event compileEvent = new org.osgi.service.event.Event(
							WebResourceInventoryManager.COMPILE_EVENT,
							properties);
					eventAdmin.sendEvent(compileEvent);
				}
			} catch (RepositoryException e) {

			}

		}

		private boolean ignoreInventoryEvent(
				org.osgi.service.event.Event event, Node resourceNode)
				throws RepositoryException {
			boolean skipSweep = false;

			String path = null;
			String eventTopic = event.getTopic();
			path = (String) event.getProperty("path");
			// We don't want to trigger a sweep if only the
			// group or inventory was changed
			if ( path.endsWith(WebResourceGroup.GROUP_HASH)||
				 path.endsWith(WebResourceGroup.INVENTORY)|| 
				 path.endsWith(".css") || path.endsWith(".js")) {
				skipSweep = true;
			} else {
				if (!resourceNode.isNodeType(NodeType.NT_FILE)) {
					skipSweep = true;
				}
			}
			return skipSweep;
		}
	}

	class WebResourceGroupFolderHandler implements EventHandler {
		@Override
		public void handleEvent(org.osgi.service.event.Event event) {

			try {

				String path = (String) event.getProperty("path");
				String eventTopic = event.getTopic();
				Dictionary<String, Object> properties = new Hashtable<String, Object>();
				properties.put("path", path);
				org.osgi.service.event.Event osgiEvent = null;
				if (eventTopic.equals(SlingConstants.TOPIC_RESOURCE_ADDED)) {

					Node addedNode = adminSession.getNode(path);
					if (addedNode.isNodeType(WebResourceGroup.NODE_TYPE)) {
						log.info("Web Resource Add/Move for path: " + path);
						osgiEvent = new org.osgi.service.event.Event(
								TOPIC_WEB_RESOURCE_CREATED, properties);
					}

				} else {
					for (Entry<String, String> webResourceNamePathEntry : webResourceNamePathMap
							.entrySet()) {
						if (webResourceNamePathEntry.getValue().equals(path)) {
							log.info("Web Resource Delete for path: " + path);
							properties.put(WebResourceGroup.NAME,
									webResourceNamePathEntry.getKey());
							osgiEvent = new org.osgi.service.event.Event(
									TOPIC_WEB_RESOURCE_DELETED, properties);
						}
					}

				}

				if (osgiEvent != null) {
					eventAdmin.sendEvent(osgiEvent);
				}

			} catch (RepositoryException e) {
				log.error("Error on JCR event listener", e);
			}

		}
	}

	class WebResourceGroupEventHandler implements EventHandler {
		@Override
		public void handleEvent(org.osgi.service.event.Event event) {
			String eventTopic = event.getTopic();
			String path = (String) event.getProperty("path");
			try {

				if (eventTopic.equals(TOPIC_WEB_RESOURCE_CREATED)) {
					Node webResourceNode = adminSession.getNode(path);
					String webResourceNodeName = webResourceNode.getProperty(
							WebResourceGroup.NAME).getString();
					webResourceNamePathMap.put(webResourceNodeName, path);
					registerWebResourceGroupListener(webResourceNodeName, path);
					buildInventory(webResourceNodeName);
				} else if (eventTopic.equals(TOPIC_WEB_RESOURCE_DELETED)) {
					String webResourceNodeName = (String) event
							.getProperty(WebResourceGroup.NAME);
					webResourceNamePathMap.remove(webResourceNodeName);
					unregisterWebResourceGroupListener(webResourceNodeName);
				}
			} catch (RepositoryException e) {
				log.error("Could not process Web Resource Update: "
						+ eventTopic + " at: " + path, e);
			}

		}
	}

	@Override
	public List<String> getSourceWebResources(String webResourceName) {
		List<String> result = new ArrayList<String>();
		Map<String, List<String>> extentionMap = webResourceExtentionInventoryMap
				.get(webResourceName);
		for (List<String> currentResourceList : extentionMap.values()) {
			result.addAll(currentResourceList);
		}
		return result;
	}

	@Override
	public String getWebResourcePathLookup(String webResourceName) {
		return webResourceNamePathMap.get(webResourceName);
	}
	
	@Override
	public Set<String> getAllWebResourceNames() {
		return this.webResourceNamePathMap.keySet();
	}
	
	@Override
	public Collection<String> getAllWebResourcePaths() {
		return this.webResourceNamePathMap.values();
	}

	protected void deactivate(ComponentContext context) {
		if (adminSession != null) {
			adminSession.logout();
		}
	}
}
