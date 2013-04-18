package org.apache.sling.webresource.taglib;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.scripting.SlingScriptHelper;

import org.apache.sling.webresource.WebResourceScriptCache;

public class WebResourceTagTest {

    @Test
    public void testDoStartTagBadSetup() throws Exception {
        WebResourceTag testTag = new WebResourceTag();
        try{
            testTag.doStartTag();
            fail("Path and Web Resource Group should not be null");
        }catch(JspException e)
        {
            //Exception expected.
        }
    }
    
    @Test
    public void testDoStartTagBadSetupBoth() throws Exception {
        WebResourceTag testTag = new WebResourceTag();
        try{
            testTag.setPath("test");
            testTag.setGroupName("Test");
            testTag.doStartTag();
            fail("Path and Web Resource Group should not both be set");
        }catch(JspException e)
        {
            //Exception expected.
        }
    }
    
    @Test
    public void testDoStartTagPath() throws Exception {
        WebResourceTag testTag = new WebResourceTag();
        testTag.setPath("test");
        try{
            testTag.doStartTag();
            
        }catch(JspException e)
        {
            fail("Path set should pass");
        }
    }
    
    @Test
    public void testDoStartTagWebGroup() throws Exception {
        WebResourceTag testTag = new WebResourceTag();
        testTag.setGroupName("test");
        try{
            testTag.doStartTag();
            
        }catch(JspException e)
        {
            fail("Group set should pass");
        }
    }

    @Test
    public void testDoEndTagPath() throws Exception {
        
        PageContext mockPageContext = createMock(PageContext.class);
        SlingScriptHelper mockSlingScript = createMock(SlingScriptHelper.class);
        Node mockNode = createMock(Node.class);
        WebResourceScriptCache mockWebResourceCache = createMock(WebResourceScriptCache.class);
        JspWriter mockJspWriter = createMock(JspWriter.class);
        Session mockSession = createMock(Session.class);
        
        expect(mockPageContext.findAttribute("sling")).andReturn(mockSlingScript);
        expect(mockPageContext.findAttribute("currentNode")).andReturn(mockNode);
        expect(mockSlingScript.getService(WebResourceScriptCache.class)).andReturn(mockWebResourceCache);
        
        expect(mockPageContext.getOut()).andReturn(mockJspWriter);
        expect(mockNode.getSession()).andReturn(mockSession);
        
        expect(mockWebResourceCache.getCompiledScriptPath(mockSession, "/test/source.coffee")).andReturn("/var/coffee/test/source.js");
        mockJspWriter.write("<script src=\"/var/coffee/test/source.js\"></script>");
        
        replay(mockPageContext);
        replay(mockSlingScript);
        replay(mockNode);
        replay(mockWebResourceCache);
        replay(mockJspWriter);
        replay(mockSession);
        
        WebResourceTag testTag = new WebResourceTag();
        testTag.setPageContext(mockPageContext);
        testTag.setPath("/test/source.coffee");
        
        testTag.doStartTag();
        testTag.doEndTag();
        
        verify(mockPageContext);
        verify(mockSlingScript);
        verify(mockNode);
        verify(mockWebResourceCache);
        verify(mockJspWriter);
        verify(mockSession);
        
    }
    
    @Test
    public void testDoEndTagGroup() throws Exception {
        
        PageContext mockPageContext = createMock(PageContext.class);
        SlingScriptHelper mockSlingScript = createMock(SlingScriptHelper.class);
        Node mockNode = createMock(Node.class);
        WebResourceScriptCache mockWebResourceCache = createMock(WebResourceScriptCache.class);
        JspWriter mockJspWriter = createMock(JspWriter.class);
        Session mockSession = createMock(Session.class);
        
        expect(mockPageContext.findAttribute("sling")).andReturn(mockSlingScript);
        expect(mockPageContext.findAttribute("currentNode")).andReturn(mockNode);
        expect(mockSlingScript.getService(WebResourceScriptCache.class)).andReturn(mockWebResourceCache);
        
        expect(mockPageContext.getOut()).andReturn(mockJspWriter);
        expect(mockNode.getSession()).andReturn(mockSession);
        
        List<String> groupPaths = new ArrayList<String>();
        groupPaths.add("/content/test/source1.js");
        groupPaths.add("/content/test/source2.js");
        
        expect(mockWebResourceCache.getCompiledWebResourceGroupPaths(mockSession, "test", false)).andReturn(groupPaths);
        mockJspWriter.write("<script src=\"/content/test/source1.js\"></script>");
        mockJspWriter.write("<script src=\"/content/test/source2.js\"></script>");
        
        replay(mockPageContext);
        replay(mockSlingScript);
        replay(mockNode);
        replay(mockWebResourceCache);
        replay(mockJspWriter);
        replay(mockSession);
        
        WebResourceTag testTag = new WebResourceTag();
        testTag.setPageContext(mockPageContext);
        testTag.setGroupName("test");
        
        testTag.doStartTag();
        testTag.doEndTag();
        
        verify(mockPageContext);
        verify(mockSlingScript);
        verify(mockNode);
        verify(mockWebResourceCache);
        verify(mockJspWriter);
        verify(mockSession);
        
    }
}
