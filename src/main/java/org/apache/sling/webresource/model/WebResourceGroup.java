package org.apache.sling.webresource.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class WebResourceGroup {
    
	public static final String NODE_TYPE = "webresource:WebResourceGroup";
	
    public static final String NAME = "webresource:name";
    
    public static final String CACHE_PATH = "webresource:cachePath";
    
    public static final String GROUP_HASH = "webresource:groupHash";
    
    public static final String INVENTORY = "inventory";
    
    private String name;
    
    private String groupPath;
    
    private String cachePath;
    
    private String groupHash;
    
    private Map<String, List<String>> inventory;
    
    private Map<String, Object> compileOptions;
    
    public WebResourceGroup() {
        compileOptions = new HashMap<String, Object>();
        inventory = new HashMap<String, List<String>>();
    }
    
    public WebResourceGroup(Node webResourceGroup) throws RepositoryException
    {
        this();
        
        this.groupPath = webResourceGroup.getPath();
        
        if(webResourceGroup.hasProperty(NAME))
        {
            this.name = webResourceGroup.getProperty(NAME).getString();
        }
        
        if(webResourceGroup.hasProperty(CACHE_PATH))
        {
            this.cachePath = webResourceGroup.getProperty(CACHE_PATH).getString();
        }
        
        if(webResourceGroup.hasProperty(GROUP_HASH))
        {
        	this.groupHash = webResourceGroup.getProperty(GROUP_HASH).getString();
        }
        
        if(webResourceGroup.hasNode(INVENTORY))
        {
        	Node inventoryNode = webResourceGroup.getNode(INVENTORY);
        	PropertyIterator inventoryPropIt = inventoryNode.getProperties();
        	while(inventoryPropIt.hasNext())
        	{
        		Property currentInventoryProperty = inventoryPropIt.nextProperty();
        		if(!currentInventoryProperty.getName().startsWith("jcr:"))
        		{
        			String inventoryType = currentInventoryProperty.getName();
            		if(!inventory.containsKey(inventoryType) )
            		{
            			inventory.put(inventoryType, new ArrayList<String>());
            		}
            		List<String> inventoryTypeList = inventory.get(inventoryType);
            		Value[] inventoryTypeValues = currentInventoryProperty.getValues();
            		for(Value currentValue: inventoryTypeValues)
            		{
            			inventoryTypeList.add(currentValue.getString());
            		}
        		}
        	}
        }
        
        if(webResourceGroup.hasNode("compileOptions"))
        {
            Node compileOptionsNode = webResourceGroup.getNode("compileOptions");
            NodeIterator compileOptionIt = compileOptionsNode.getNodes();
            
            while(compileOptionIt.hasNext())
            {
                Node currentCompileOptionNode = compileOptionIt.nextNode();
                String compilerName = currentCompileOptionNode.getProperty("webresource:compiler").getString();
                Map<String, Object> currentCompilerOptions = (Map<String, Object>)compileOptions.get(compilerName);
                if(currentCompilerOptions == null)
                {
                    currentCompilerOptions = new HashMap<String, Object>();
                    compileOptions.put(compilerName, currentCompilerOptions);
                }
                
                String optionName = currentCompileOptionNode.getProperty("webresource:compileOptionName").getString();
                String optionValue = currentCompileOptionNode.getProperty("webresource:compileOptionValue").getString();
                
                currentCompilerOptions.put(optionName, optionValue);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getGroupPath() {
        return groupPath;
    }
    
    public void setGroupPath(String groupPath) {
        this.groupPath = groupPath;
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }
    
    public Map<String, List<String>> getInventory()
    {
    	return this.inventory;
    }
    
    public String groupHash()
    {
    	return this.groupHash;
    }

    public Map<String, Object> getCompileOptions() {
        return compileOptions;
    }

    public void setCompileOptions(Map<String, Object> compileOptions) {
        this.compileOptions = compileOptions;
    }

}
