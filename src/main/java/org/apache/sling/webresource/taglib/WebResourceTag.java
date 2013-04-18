package org.apache.sling.webresource.taglib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.scripting.SlingScriptHelper;

import org.apache.sling.webresource.WebResourceScriptCache;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.exception.WebResourceCompilerNotFoundException;
import org.apache.sling.webresource.util.JCRUtils;

import org.apache.commons.io.IOUtils;

/**
 * 
 * This is a custom tag that renders a compiled web resource based on a path to
 * a web resource source file.
 * 
 * @author bpaulin
 * 
 */
public class WebResourceTag extends TagSupport {

    private String path;

    private SlingScriptHelper sling;

    private Node currentNode;

    private WebResourceScriptCache webResourceScriptCache;

    private String groupName;

    private boolean consolidate;

    private boolean shouldThrowException;

    private boolean shouldInline;

    private boolean compileOnly;

    @Override
    public int doStartTag() throws JspException {
        if ((groupName == null && path == null)
                || (groupName != null && path != null)) {
            throw new JspException("Either Group Name or Path must be set");
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
            List<String> webResourcePaths = null;
            if (groupName != null) {
                webResourcePaths = webResourceScriptCache
                        .getCompiledWebResourceGroupPaths(currentSession,
                                groupName, consolidate);
            } else if (path != null) {
                webResourcePaths = new ArrayList<String>();
                webResourcePaths.add(webResourceScriptCache
                        .getCompiledScriptPath(currentSession, path));
            }
            if (!compileOnly) {
                for (String currentPath : webResourcePaths) {
                    StringBuffer scriptBuffer = createLinks(currentSession,
                            currentPath);
                    out.write(scriptBuffer.toString());
                }
            }
        } catch (WebResourceCompileException e) {
            if (shouldThrowException) {
                throw new JspException(e);
            }
            try {
                out.write("/*" + e.toString() + "*/");
            } catch (IOException e1) {
                throw new JspException(e1);
            }
        } catch (WebResourceCompilerNotFoundException e) {
            if (shouldThrowException) {
                throw new JspException(e);
            }
            try {
                out.write("/*" + e.toString() + "*/");
            } catch (IOException e1) {
                throw new JspException(e1);
            }
        } catch (IOException e) {
            try {
                out.write("/*" + e.toString() + "*/");
            } catch (IOException e1) {
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
     * Build links to compiled resources
     * 
     * @param currentSession
     * @param currentPath
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    protected StringBuffer createLinks(Session currentSession,
            String currentPath) throws RepositoryException, IOException {
        StringBuffer scriptBuffer = new StringBuffer();
        if (currentPath.endsWith(".css")) {
            scriptBuffer.append("<link rel=\"stylesheet\" ");
            if (shouldInline) {
                scriptBuffer.append(">");
                copyCompiledNodeToBuffer(currentSession, currentPath,
                        scriptBuffer);
                scriptBuffer.append("</link>");
            } else {
                scriptBuffer.append("href=\"" + currentPath + "\"/>");
            }

        } else {
            scriptBuffer.append("<script");
            if (shouldInline) {
                scriptBuffer.append(">");
                copyCompiledNodeToBuffer(currentSession, currentPath,
                        scriptBuffer);
                scriptBuffer.append("</script>");

            } else {
                scriptBuffer.append(" src=\"" + currentPath + "\"></script>");
            }
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

    public String getPath() {
        return path;
    }

    public boolean shouldThrowException() {
        return shouldThrowException;
    }

    public void setShouldThrowException(boolean shouldThrowException) {
        this.shouldThrowException = shouldThrowException;
    }

    public void setShouldInline(boolean shouldInline) {
        this.shouldInline = shouldInline;
    }

    public boolean shouldInline() {
        return shouldInline;
    }
    
    public void setPath(String path) {
        this.path = path;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setConsolidate(boolean consolidate) {
        this.consolidate = consolidate;
    }

    public void setCompileOnly(boolean compileOnly) {
        this.compileOnly = compileOnly;
    }

}
