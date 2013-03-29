package org.apache.sling.webresource.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
/**
 * 
 * JCR Utility Class to manipulate various things while dealing with the JCR
 * 
 * @author bpaulin
 *
 */
public class JCRUtils {
    /**
     * 
     * Recursively create nodes to a given path.
     * 
     * @param parent
     * @param path
     * @return
     * @throws RepositoryException
     */
    public static Node createNode(Node parent, String path) throws RepositoryException
    {
        path = convertPathToRelative(path);
        int pathPos = path.indexOf("/");
        String currentNodeName = path;
        if(pathPos > 0)
        {
            currentNodeName = path.substring(0, pathPos);
        }
        
        Node newParent = null;
        if(parent.hasNode(currentNodeName))
        {
            newParent = parent.getNode(currentNodeName);
        }
        else
        {
            newParent = parent.addNode(currentNodeName);
        }
        
        if(pathPos < 0)
        {
            return newParent;
        }
        else
        {
            return createNode(newParent, path.substring(pathPos+ 1));
        }
    }
    /**
     * 
     * Convert an absolute path to a relative path
     * 
     * @param path
     * @return
     */
    public static String convertPathToRelative(String path) {
        if(path.startsWith("/"))
        {
            path = path.substring(1);
        }
        return path;
    }

}
