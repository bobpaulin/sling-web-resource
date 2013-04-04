package org.apache.sling.webresource.util;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
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
    public static Node createNode(Node parent, String path)
            throws RepositoryException {
        path = convertPathToRelative(path);
        int pathPos = path.indexOf("/");
        String currentNodeName = path;
        if (pathPos > 0) {
            currentNodeName = path.substring(0, pathPos);
        }

        Node newParent = null;
        if (parent.hasNode(currentNodeName)) {
            newParent = parent.getNode(currentNodeName);
        } else {
            newParent = parent.addNode(currentNodeName);
        }

        if (pathPos < 0) {
            return newParent;
        } else {
            return createNode(newParent, path.substring(pathPos + 1));
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
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
    /**
     * 
     * Returns the extention of the node.
     * 
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static String getNodeExtension(Node node) throws RepositoryException
    {
        String nodeName = node.getName();
        int extensionPosition = nodeName.lastIndexOf(".");
        return nodeName.substring(extensionPosition + 1);
    }
    
    /**
     * 
     * Converts a Node's Path to the same Path with a new extension.
     * 
     * @param node
     * @param extension
     * @return
     * @throws RepositoryException
     */
    public static String convertNodeExtensionPath(Node node, String extension) throws RepositoryException
    {
        String oldExtension = getNodeExtension(node);
        String oldPath = node.getPath();
        int extensionPos = oldPath.lastIndexOf(oldExtension);
        
        return oldPath.substring(0, extensionPos) + extension; 
    }
    
    /**
     * 
     * Converts a file node to a InputStream
     * 
     * @param fileNode
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     * @throws ValueFormatException
     */
    public static InputStream getFileNodeAsStream(Node fileNode)
            throws RepositoryException{
        Node webResourceContent = fileNode.getNode(Property.JCR_CONTENT);
        Property webResourceData = webResourceContent
                .getProperty(Property.JCR_DATA);

        return webResourceData.getBinary().getStream();
    }

}
