package org.apache.sling.webresource.taglib;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.webresource.WebResourceScriptCache;
import org.apache.sling.webresource.util.JCRUtils;

public class WebResourceTag extends TagSupport {

    /**
	 * 
	 */
	private static final long serialVersionUID = -3322071843929377647L;


    private SlingScriptHelper sling;

    private Node currentNode;

    private WebResourceScriptCache webResourceScriptCache;

    private String groupName;

    private boolean inline;

    @Override
    public int doStartTag() throws JspException {
        if (groupName == null) {
            throw new JspException("Either Group Name must be set");
        }

        return super.doStartTag();
    }

    @Override
    public void setPageContext(PageContext pageContext) {

        super.setPageContext(pageContext);
        sling = (SlingScriptHelper) pageContext.findAttribute("sling");
        currentNode = (Node) pageContext.findAttribute("currentNode");
        webResourceScriptCache = sling.getService(WebResourceScriptCache.class);
    }

    @Override
    public int doEndTag() throws JspException {
        JspWriter out = null;
        try {
            out = pageContext.getOut();
            Session currentSession = currentNode.getSession();
            Map<String, List<String>> webResourcePaths = null;
            if (groupName != null) {
                webResourcePaths = webResourceScriptCache
                        .getWebResourceCachedInventoryPaths(currentSession, groupName);
               for(String currentExtention: webResourcePaths.keySet())
               {
            	   processExtentionList(out, currentSession, webResourcePaths, currentExtention);
               }
               
            }
        }catch(IOException e)
        {
        	try{
        		out.write("/*" + e.toString() + "*/");
        	}catch(IOException e1)
        	{
        		throw new JspException(e1);
        	}
        } catch (RepositoryException e) {
            try {
                out.write("/*" + e.toString() + "*/");
            } catch (IOException e1) {
                throw new JspException(e1);
            }
        }

        return super.doEndTag();
    }
    /**
     * 
     * Writes out scripts based on extension.
     * 
     * @param out
     * @param currentSession
     * @param webResourcePathMap
     * @param extentionName
     * @throws IOException
     * @throws RepositoryException
     */
    protected void processExtentionList(JspWriter out, Session currentSession,
            Map<String, List<String>> webResourcePathMap, String extentionName)
            throws IOException, RepositoryException {
        if(extentionName.equals("js"))
        {
            for (String currentPath : webResourcePathMap.get(extentionName)) {
                StringBuffer scriptBuffer = createScriptLinks(currentSession,
                        currentPath);
                out.write(scriptBuffer.toString());
            }
        }
        else
        {
            for (String currentPath : webResourcePathMap.get(extentionName)) {
                StringBuffer scriptBuffer = createStyleSheetLinks(currentSession,
                        currentPath);
                out.write(scriptBuffer.toString());
            }
        }
    }
    
    protected StringBuffer createStyleSheetLinks(Session currentSession,
            String currentPath) throws RepositoryException, IOException {
        StringBuffer scriptBuffer = new StringBuffer();
        
            scriptBuffer.append("<link rel=\"stylesheet\" ");
            if (inline) {
                scriptBuffer.append(">");
                copyCompiledNodeToBuffer(currentSession, currentPath,
                        scriptBuffer);
                scriptBuffer.append("</link>");
            } else {
                scriptBuffer.append("href=\"" + currentPath + "\"/>");
            }

        return scriptBuffer;
    }
    
    protected StringBuffer createScriptLinks(Session currentSession,
            String currentPath) throws RepositoryException, IOException {
        StringBuffer scriptBuffer = new StringBuffer();
        scriptBuffer.append("<script");
        if (inline) {
            scriptBuffer.append(">");
            copyCompiledNodeToBuffer(currentSession, currentPath,
                    scriptBuffer);
            scriptBuffer.append("</script>");

        } else {
            scriptBuffer.append(" src=\"" + currentPath + "\"></script>");
        }
        
        return scriptBuffer;
    }

    /**
     * 
     * Copies the full node content to the buffer. Used for inlining
     * 
     * @param currentSession
     * @param currentPath
     * @param scriptBuffer
     * @throws RepositoryException
     * @throws IOException
     */
    protected void copyCompiledNodeToBuffer(Session currentSession,
            String currentPath, StringBuffer scriptBuffer)
            throws RepositoryException, IOException {
        Node compiledNode = currentSession.getNode(currentPath);
        String compiledScriptString = IOUtils.toString(JCRUtils
                .getFileNodeAsStream(compiledNode));
        scriptBuffer.append(compiledScriptString);
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public boolean getInline() {
        return inline;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

}
