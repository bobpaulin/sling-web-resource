package org.apache.sling.webresource.util;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Binary;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

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
		path = convertPathToRelative("/", path);
		int pathPos = path.indexOf("/");
		String currentNodeName = path;
		if (pathPos > 0) {
			currentNodeName = path.substring(0, pathPos);
		}

		Node newParent = null;
		if (parent.hasNode(currentNodeName)) {
			newParent = parent.getNode(currentNodeName);
		} else if(pathPos < 0 && path.indexOf(".") > 0){
			newParent = parent.addNode(currentNodeName, NodeType.NT_FILE);
		} else {
			newParent = parent.addNode(currentNodeName, NodeType.NT_FOLDER);
		}

		if (pathPos < 0) {
			return newParent;
		} else {
			return createNode(newParent, path.substring(pathPos + 1));
		}
	}

	/**
	 * 
	 * Converts a path to a relative path to the base path.
	 * 
	 * @param basePath
	 * @param path
	 * @return
	 */
	public static String convertPathToRelative(String basePath, String path) {
		if (path.startsWith(basePath)) {
			return path.substring(basePath.length());
		} else {
			return path;
		}
	}

	/**
	 * 
	 * Returns the extention of the node.
	 * 
	 * @param node
	 * @return
	 * @throws RepositoryException
	 */
	public static String getNodeExtension(Node node) throws RepositoryException {
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
	public static String convertNodeExtensionPath(Node node, String extension)
			throws RepositoryException {
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
			throws RepositoryException {
		Node webResourceContent = fileNode.getNode(Property.JCR_CONTENT);
		Property webResourceData = webResourceContent
				.getProperty(Property.JCR_DATA);

		return webResourceData.getBinary().getStream();
	}

	public static InputStream getFileResourceAsStream(
			ResourceResolver resolver, String path) throws RepositoryException {
		Resource fileResource = resolver.getResource(path);
		return getFileNodeAsStream(fileResource.adaptTo(Node.class));
	}

	public static void createFileContentNode(String destinationPath,
			InputStream result, Session session) throws RepositoryException {
		Node compiledNode = JCRUtils.createNode(session.getRootNode(),
				destinationPath);

		compiledNode.setPrimaryType("nt:file");
		Node compiledContent = null;
		if (compiledNode.hasNode(Property.JCR_CONTENT)) {
			compiledContent = compiledNode.getNode(Property.JCR_CONTENT);
		} else {
			compiledContent = compiledNode.addNode(Property.JCR_CONTENT,
					"nt:resource");
		}

		createBinaryJCRData(result, session, compiledContent);
	}

	/**
	 * 
	 * Creates binary data from an input stream.
	 * 
	 * @param result
	 * @param session
	 * @param compiledContent
	 */
	public static void createBinaryJCRData(InputStream result, Session session,
			Node compiledContent) throws RepositoryException {
		ValueFactory valueFactory = session.getValueFactory();
		Binary compiledBinary = valueFactory.createBinary(result);

		compiledContent.setProperty(Property.JCR_DATA, compiledBinary);
		Calendar lastModified = Calendar.getInstance();
		compiledContent.setProperty(Property.JCR_LAST_MODIFIED, lastModified);
	}

	/**
	 * 
	 * Retrieves JCR File node's last modified date.
	 * 
	 * @param fileNode
	 * @return
	 */
	public static Calendar getJcrModifiedDate(Node fileNode)
			throws RepositoryException {
		Node contentNode = fileNode.getNode(Property.JCR_CONTENT);

		Property lastModifiedProperty = contentNode
				.getProperty(Property.JCR_LAST_MODIFIED);
		return lastModifiedProperty.getDate();
	}

}
