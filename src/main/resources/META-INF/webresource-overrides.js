importPackage(java.io);
importPackage(java.nio.charset);
importPackage(Packages.org.apache.sling.webresource.util);
importPackage(Packages.org.apache.sling.webresource);
importPackage(Packages.javax.jcr);
importPackage(Packages.org.apache.commons.io);
/**
 * Overrides to enable system to use sling functionality for: 
 * File Reads
 * 
 * @author bpaulin
 * 
 */

if (typeof String.prototype.startsWith != 'function') {
	  String.prototype.startsWith = function (str){
	    return this.slice(0, str.length) == str;
	  };
	}

/**
 * Overriding default Rhino readFile.
 * Reads a JCR File
 * 
 * @param filename
 */
function readFile(filename, characterCoding)
{
	if(!characterCoding)
	{
		characterCoding = Charset.defaultCharset();
	}
	var fileNode = null;
	if(filename.startsWith('webresource://'))
	{
		//Implement Web Resource stuff here.
		filename = filename.replace('webresource://', '');
		webResourceName = filename.slice(0, filename.indexOf('/'));
		var webResourcePath = webResourceInventoryManager.getWebResourcePathLookup(webResourceName);
		filename = filename.replace(webResourceName, webResourcePath);
		fileNode = currentNode.getSession().getNode(filename);
	}
	else
	{
		fileNode = currentNode.getNode(filename);
	}
	
	var inputStream = JCRUtils.getFileNodeAsStream(fileNode);
	
	return String(IOUtils.toString(inputStream, characterCoding));
}