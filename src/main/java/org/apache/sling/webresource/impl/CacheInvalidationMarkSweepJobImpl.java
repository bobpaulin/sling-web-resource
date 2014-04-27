package org.apache.sling.webresource.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.webresource.WebResourceScriptCache;
import org.apache.sling.webresource.model.WebResourceGroup;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service
public class CacheInvalidationMarkSweepJobImpl implements Runnable {
	@Property(longValue = 600)
	public static final String SCHEDULER_PERIOD = "scheduler.period";

	@Reference
	private SlingRepository repository;

	@Reference
	private WebResourceScriptCache webResourceScriptCache;

	@Reference
	private EventAdmin eventAdmin;

	private Session adminSession;

	private Map<String, EventListener> pathEventListenerMap;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public static final String COMPILE_EVENT = "org/apache/sling/webresource/COMPILE";

	protected void activate(ComponentContext context) {

		pathEventListenerMap = new HashMap<String, EventListener>();

		try {
			adminSession = repository.loginAdministrative(null);
		} catch (RepositoryException e) {
			log.error("Could not Login to admin session", e);
		}

		// Initialize event listeners and do initial sweep for resources needing
		// compilation
		run();
	}

	private void registerWebResourceGroupListener(final String webResourceName, final String webResourcePath)
			throws RepositoryException {
		if (!pathEventListenerMap.containsKey(webResourceName)) {
			EventListener eventListener = new EventListener() {

				private String eventWebResourceName = webResourceName;

				@Override
				public void onEvent(EventIterator eventIterator) {
					boolean skipSweep = excludeEventFromSweep(eventIterator);
					if (!skipSweep) {
						runMarkSweep(Collections.singletonList(this.eventWebResourceName));
					}
				}

				private boolean excludeEventFromSweep(
						EventIterator eventIterator) {
					boolean skipSweep = false;
					while (eventIterator.hasNext()) {
						Event currentEvent = eventIterator.nextEvent();
						String path = null;
						try {
							path = currentEvent.getPath();
							int eventType = currentEvent.getType();
							// We don't want to trigger a sweep if only the
							// cache path or inventory was changed
							if (eventType == Event.NODE_ADDED
									&& path.endsWith(WebResourceGroup.INVENTORY)
									|| ((eventType == Event.PROPERTY_ADDED || eventType == Event.PROPERTY_CHANGED) && path
											.endsWith(WebResourceGroup.CACHE_PATH))) {
								skipSweep = true;
							}
						} catch (RepositoryException e) {
							log.warn(
									"Error retrieving path for skipping sweep",
									e);
						}

					}
					return skipSweep;
				}
			};

			this.pathEventListenerMap.put(webResourceName, eventListener);
			adminSession
					.getWorkspace()
					.getObservationManager()
					.addEventListener(

							eventListener, // handler

							Event.PROPERTY_ADDED | Event.NODE_ADDED
									| Event.NODE_MOVED | Event.NODE_REMOVED
									| Event.PROPERTY_CHANGED
									| Event.PROPERTY_REMOVED, // binary
																// combination
																// of event
																// types

							webResourcePath, // path

							true, // is Deep?

							null, // uuids filter

							null, // nodetypes filter
							true);
			log.info("Registered Cache Invalidation Event Listener");
		}

	}

	@Override
	public void run() {
		runMarkSweep(null);

	}

	private void runMarkSweep(List<String> webResourceFilterList) {
		// Find all web resource nodes
		log.info("Starting Mark Sweep");
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();

		try {
			if (adminSession != null) {
				ValueFactory vf = adminSession.getValueFactory();

				QueryObjectModelFactory qf = adminSession.getWorkspace()
						.getQueryManager().getQOMFactory();
				Constraint constraints = null;
				if (CollectionUtils.isNotEmpty(webResourceFilterList)) {
					for (String currentWebResourceName : webResourceFilterList) {
						if (constraints == null) {
							constraints = qf
									.comparison(
											qf.propertyValue(
													"webResourceGroup",
													WebResourceGroup.NAME),
											QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
											qf.literal(vf
													.createValue(currentWebResourceName)));
						} else {
							constraints = qf
									.or(constraints,
											qf.comparison(
													qf.propertyValue(
															"webResourceGroup",
															WebResourceGroup.NAME),
													QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
													qf.literal(vf
															.createValue(currentWebResourceName))));
						}
					}
				}
				Query jcrQuery = qf.createQuery(qf.selector(
						"webresource:WebResourceGroup", "webResourceGroup"),
						constraints, null, null);
				log.info("QOM Query: " + jcrQuery.getStatement());
				StopWatch queryStopWatch = new StopWatch();
				queryStopWatch.start();
				QueryResult result = jcrQuery.execute();
				queryStopWatch.stop();
				log.info("Mark Sweep Query Complete in: " + stopwatch);

				NodeIterator resultIt = result.getNodes();

				List<String> paths = new ArrayList<String>();

				while (resultIt.hasNext()) {

					Node webResourceNode = resultIt.nextNode();
					String webResourceNodePath = webResourceNode.getPath();
					String webResourceName = webResourceNode.getProperty(
							WebResourceGroup.NAME).getString();
					registerWebResourceGroupListener(webResourceName, webResourceNodePath);
					

					String newGroupHash = webResourceScriptCache
							.calculateWebResourceGroupHash(adminSession,
									webResourceName);
					String oldGroupHash = null;
					boolean groupHashExists = webResourceNode
							.hasProperty(WebResourceGroup.GROUP_HASH);
					if (groupHashExists) {
						oldGroupHash = webResourceNode.getProperty(
								WebResourceGroup.GROUP_HASH).getString();
					}

					if (!newGroupHash.equals(oldGroupHash)) {
						log.info("Mark web resource: " + webResourceName
								+ " for recompilation.");
						if (groupHashExists) {
							webResourceNode.getProperty(
									WebResourceGroup.GROUP_HASH).remove();
							adminSession.save();
						}
						// Add to list

						paths.add(webResourceNodePath);
					}
				}
				Dictionary<String, Object> properties = new Hashtable<String, Object>();
				properties.put("paths", paths);
				org.osgi.service.event.Event compileEvent = new org.osgi.service.event.Event(
						COMPILE_EVENT, properties);
				eventAdmin.sendEvent(compileEvent);
				stopwatch.stop();
				log.info("Cache Sweep completed in: " + stopwatch);
			}
		} catch (InvalidQueryException e) {
			log.error("Sweep for Changes failed", e);
		} catch (RepositoryException e) {
			log.error("Sweep for Changes failed", e);
		}
	}

	protected void deactivate() {
		if (adminSession != null) {
			adminSession.logout();
		}
	}

}
