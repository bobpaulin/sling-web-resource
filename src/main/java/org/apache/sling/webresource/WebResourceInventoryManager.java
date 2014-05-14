package org.apache.sling.webresource;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface WebResourceInventoryManager {
	
	public static final String TOPIC_WEB_RESOURCE_CREATED = "org/apache/sling/webresource/CREATED";

	public static final String TOPIC_WEB_RESOURCE_DELETED = "org/apache/sling/webresource/DELETED";
	
	public static final String COMPILE_EVENT = "org/apache/sling/webresource/COMPILE";
	
	public static final String COMPILE_ALL_EVENT = "org/apache/sling/webresource/COMPILEALL";
	
	public List<String> getSourceWebResources(String webResourceName);
	
	public String getWebResourcePathLookup(String webResourceName);
	
	public Set<String> getAllWebResourceNames();
	
	public Collection<String> getAllWebResourcePaths();

}
