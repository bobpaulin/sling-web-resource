package org.apache.sling.webresource.compiler.impl;

import java.io.InputStream;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.util.JCRUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Abstract NoOp class for processing JS and CSS files.
 * 
 * @author bpaulin
 * 
 */
public abstract class AbstractNoOpCompiler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 
     * This should not be called on a CSS or JS file.
     * 
     * @param webResourceScript
     * @return
     * @throws WebResourceCompileException
     */
    public InputStream compile(InputStream webResourceScript)
            throws WebResourceCompileException {
        return compile(webResourceScript, null);
    }

    /**
     * 
     * This should not be called on a CSS or JS file.
     * 
     * @param webResourceScript
     * @param compileOptions
     * @return
     * @throws WebResourceCompileException
     */
    public InputStream compile(InputStream webResourceScript,
            Map<String, Object> compileOptions)
            throws WebResourceCompileException {
        throw new WebResourceCompileException(
                "NoOp compile method should never be called");
    }

    public String getCacheRoot() {
        return "/";
    }

    public boolean canCompileNode(Node sourceNode) {
        String extension = null;
        try {

            extension = JCRUtils.getNodeExtension(sourceNode);

        } catch (RepositoryException e) {
            // Log Exception
            log.info("Node Name can not be read.  Skipping node.");
        }

        return compiledScriptExtension().equals(extension);
    }

    public abstract String compiledScriptExtension();
}
