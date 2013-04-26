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

/**
 * 
 * No Op class for JavaScript. Nothing needs to be compiled here.
 * 
 * @author bpaulin
 * 
 */

@Component(label="JavaScript No Op Compiler Service", immediate = true)
@Service
public class JavaScriptNoOpCompilerImpl extends AbstractNoOpCompiler implements
        WebResourceScriptCompiler {
    @Override
    public String compiledScriptExtension() {
        return "js";
    }
}
