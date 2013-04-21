package org.apache.sling.webresource.model;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class WebResourceGroup {
    
    public static final String NAME = "webresource:name";
    
    public static final String CACHE_PATH = "webresource:cachePath";
    
    private String name;
    
    private String groupPath;
    
    private String cachePath;
    
    private Map<String, Object> compileOptions;
    
    public WebResourceGroup() {
        compileOptions = new HashMap<String, Object>();
    }
    
    public WebResourceGroup(Node webResourceGroup) throws RepositoryException
    {
        compileOptions = new HashMap<String, Object>();
        
        this.groupPath = webResourceGroup.getPath();
        
        if(webResourceGroup.hasProperty(NAME))
        {
            this.name = webResourceGroup.getProperty(NAME).getString();
        }
        
        if(webResourceGroup.hasProperty(CACHE_PATH))
        {
            this.cachePath = webResourceGroup.getProperty(CACHE_PATH).getString();
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

    public Map<String, Object> getCompileOptions() {
        return compileOptions;
    }

    public void setCompileOptions(Map<String, Object> compileOptions) {
        this.compileOptions = compileOptions;
    }

}
