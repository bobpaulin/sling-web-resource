package org.apache.sling.webresource.impl;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.util.Calendar;

import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.model.WebResourceGroup;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Property;

public class WebResourceScriptCacheImplTest {
	private WebResourceScriptCacheImpl webResourceScriptCache;

	// Mocks

	private Node mockNode;
	private WebResourceScriptCompiler mockScriptCompiler;

	@Before
	public void setUp() throws Exception {
		webResourceScriptCache = new WebResourceScriptCacheImpl();
		mockNode = createMock(Node.class);
		mockScriptCompiler = createMock(WebResourceScriptCompiler.class);

	}

	@Test
	public void testGetCachedCompiledScriptPathGroupCachePath()
			throws Exception {

		WebResourceGroup webResrouceGroup = new WebResourceGroup();
		webResrouceGroup.setCachePath("/test/cache/path");
		webResrouceGroup.setGroupPath("/test/group/path");
		webResrouceGroup.setName("testGroup");

		expect(mockScriptCompiler.compiledScriptExtension()).andReturn("js");
		expect(mockNode.getName()).andReturn("test.coffee");
		expect(mockNode.getPath()).andReturn("/test/group/path/test.coffee");

		replay(mockNode);
		replay(mockScriptCompiler);

		String result = webResourceScriptCache.getCachedCompiledScriptPath(
				mockNode, webResrouceGroup, mockScriptCompiler);

		assertEquals("Cache Path should be overriden",
				"/test/cache/path/test.js", result);

		verify(mockNode);
		verify(mockScriptCompiler);

	}

	@Test
	public void testGetCachedCompiledScriptPathGroupNoOverride()
			throws Exception {

		WebResourceGroup webResrouceGroup = new WebResourceGroup();
		webResrouceGroup.setGroupPath("/test/group/path");
		webResrouceGroup.setName("testGroup");

		expect(mockScriptCompiler.compiledScriptExtension()).andReturn("js");
		expect(mockNode.getName()).andReturn("test.coffee");
		expect(mockNode.getPath()).andReturn("/test/group/path/test.coffee");

		replay(mockNode);
		replay(mockScriptCompiler);

		String result = webResourceScriptCache.getCachedCompiledScriptPath(
				mockNode, webResrouceGroup, mockScriptCompiler);

		assertEquals("Cache Path should be group default",
				"/var/webresource/groups/test.js", result);

		verify(mockNode);
		verify(mockScriptCompiler);

	}

	@Test
	public void testGetCachedCompiledScriptPathPathBased() throws Exception {

		expect(mockScriptCompiler.compiledScriptExtension()).andReturn("js");
		expect(mockNode.getName()).andReturn("test.coffee");
		expect(mockNode.getPath()).andReturn("/test/path/test.coffee");
		expect(mockScriptCompiler.getCacheRoot()).andReturn("/compile/cache");

		replay(mockNode);
		replay(mockScriptCompiler);

		String result = webResourceScriptCache.getCachedCompiledScriptPath(
				mockNode, null, mockScriptCompiler);

		assertEquals("Cache Path should be compiler default",
				"/compile/cache/test/path/test.js", result);

		verify(mockNode);
		verify(mockScriptCompiler);

	}

	@Test
	public void testIsCacheFresh() throws Exception {

		Node mockCacheNode = createMock(Node.class);
		Node mockSourceNode = createMock(Node.class);
		Property mockSourceProperty = createMock(Property.class);
		Property mockCacheProperty = createMock(Property.class);
		Calendar olderCalendar = Calendar.getInstance();
		Calendar newerCalendar = Calendar.getInstance();
		newerCalendar.add(Calendar.MINUTE, 5);

		expect(mockCacheNode.getNode(Property.JCR_CONTENT)).andReturn(
				mockCacheNode);
		expect(mockSourceNode.getNode(Property.JCR_CONTENT)).andReturn(
				mockSourceNode);
		expect(mockCacheNode.getProperty(Property.JCR_LAST_MODIFIED))
				.andReturn(mockCacheProperty);
		expect(mockSourceNode.getProperty(Property.JCR_LAST_MODIFIED))
				.andReturn(mockSourceProperty);
		expect(mockSourceProperty.getDate()).andReturn(olderCalendar);
		expect(mockCacheProperty.getDate()).andReturn(newerCalendar);

		replay(mockCacheNode);
		replay(mockSourceNode);
		replay(mockSourceProperty);
		replay(mockCacheProperty);

		boolean result = webResourceScriptCache.isCacheFresh(mockCacheNode,
				mockSourceNode);

		assertTrue("Should be cache hit", result);

		verify(mockCacheNode);
		verify(mockSourceNode);
		verify(mockCacheProperty);
		verify(mockSourceProperty);

	}

	@Test
	public void testIsCacheNotFresh() throws Exception {

		Node mockCacheNode = createMock(Node.class);
		Node mockSourceNode = createMock(Node.class);
		Property mockSourceProperty = createMock(Property.class);
		Property mockCacheProperty = createMock(Property.class);
		Calendar olderCalendar = Calendar.getInstance();
		Calendar newerCalendar = Calendar.getInstance();
		newerCalendar.add(Calendar.MINUTE, 5);

		expect(mockCacheNode.getNode(Property.JCR_CONTENT)).andReturn(
				mockCacheNode);
		expect(mockSourceNode.getNode(Property.JCR_CONTENT)).andReturn(
				mockSourceNode);
		expect(mockCacheNode.getProperty(Property.JCR_LAST_MODIFIED))
				.andReturn(mockCacheProperty);
		expect(mockSourceNode.getProperty(Property.JCR_LAST_MODIFIED))
				.andReturn(mockSourceProperty);
		expect(mockCacheProperty.getDate()).andReturn(olderCalendar);
		expect(mockSourceProperty.getDate()).andReturn(newerCalendar);

		replay(mockCacheNode);
		replay(mockSourceNode);
		replay(mockSourceProperty);
		replay(mockCacheProperty);

		boolean result = webResourceScriptCache.isCacheFresh(mockCacheNode,
				mockSourceNode);

		assertFalse("Should be cache miss", result);

		verify(mockCacheNode);
		verify(mockSourceNode);
		verify(mockCacheProperty);
		verify(mockSourceProperty);

	}

}
