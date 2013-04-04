package org.apache.sling.webresource.compiler.impl;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.util.JCRUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNoOpCompiler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public InputStream compile(InputStream webResourceScript)
            throws WebResourceCompileException {
        throw new WebResourceCompileException("NoOp compile method should never be called");
    }

    public String getCacheRoot() {
        return "/";
    }
    
    public boolean canCompileNode(Node sourceNode) {
        String extension = null;
        try{
            
            extension = JCRUtils.getNodeExtension(sourceNode);
            
        }catch(RepositoryException e)
        {
            //Log Exception
            log.info("Node Name can not be read.  Skipping node.");
        }
        
        return compiledScriptExtension().equals(extension);
    }

    public abstract String compiledScriptExtension();
}
