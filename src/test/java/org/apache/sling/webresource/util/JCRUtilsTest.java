package org.apache.sling.webresource.util;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import javax.jcr.Node;

import org.junit.Test;

public class JCRUtilsTest {

    @Test
    public void testConvertPathToRelative() {
        String basePath = "/test/base";
        String fullPath = "/test/base/path/here";
        
        String result = JCRUtils.convertPathToRelative(basePath, fullPath);
        
        assertEquals("Path should be relative", "/path/here", result);
    }
    
    @Test
    public void testConvertPathToRelativeIncorrectBase() {
        String basePath = "/bad/base";
        String fullPath = "/test/base/path/here";
        
        String result = JCRUtils.convertPathToRelative(basePath, fullPath);
        
        assertEquals("Path should be relative", fullPath, result);
    }
    
    @Test
    public void testGetNodeExtension() throws Exception
    {
        Node mockNode = createMock(Node.class);
        
        expect(mockNode.getName()).andReturn("test.js");
        
        replay(mockNode);
        
        String result = JCRUtils.getNodeExtension(mockNode);
        
        assertEquals("Extention should be JavaScript", "js", result);
        
        verify(mockNode);
    }
    
    @Test
    public void testGetNodeExtensionAdditionalSuffix() throws Exception
    {
        Node mockNode = createMock(Node.class);
        
        expect(mockNode.getName()).andReturn("test.js.css");
        
        replay(mockNode);
        
        String result = JCRUtils.getNodeExtension(mockNode);
        
        assertEquals("Extention should be CSS", "css", result);
        
        verify(mockNode);
    }
    
    @Test
    public void testGetNodeExtensionNoExtention() throws Exception
    {
        Node mockNode = createMock(Node.class);
        
        expect(mockNode.getName()).andReturn("test");
        
        replay(mockNode);
        
        String result = JCRUtils.getNodeExtension(mockNode);
        
        assertEquals("Extention should be test", "test", result);
        
        verify(mockNode);
    }
    
    @Test
    public void testConvertNodeExtensionPath() throws Exception
    {
        Node mockNode = createMock(Node.class);
        
        expect(mockNode.getName()).andReturn("test.coffee");
        
        expect(mockNode.getPath()).andReturn("/the/path/test.coffee");
        
        replay(mockNode);
        
        String result = JCRUtils.convertNodeExtensionPath(mockNode, "js");
        
        assertEquals("Path should be changed from coffee to js", "/the/path/test.js", result);
        
        verify(mockNode);
    }
    
    @Test
    public void testCreateNode() throws Exception
    {
        Node mockNode = createMock(Node.class);
        
        expect(mockNode.hasNode("test")).andReturn(Boolean.TRUE);
        expect(mockNode.getNode("test")).andReturn(mockNode);
        
        expect(mockNode.hasNode("a")).andReturn(Boolean.TRUE);
        expect(mockNode.getNode("a")).andReturn(mockNode);
        
        expect(mockNode.hasNode("new")).andReturn(Boolean.FALSE);
        expect(mockNode.addNode("new")).andReturn(mockNode);
        
        expect(mockNode.hasNode("path.js")).andReturn(Boolean.FALSE);
        expect(mockNode.addNode("path.js")).andReturn(mockNode);
        
        replay(mockNode);
        
        JCRUtils.createNode(mockNode, "/test/a/new/path.js");
        
        verify(mockNode);
    }

}
