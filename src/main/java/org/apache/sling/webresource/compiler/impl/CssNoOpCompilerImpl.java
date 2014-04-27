package org.apache.sling.webresource.compiler.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.webresource.WebResourceScriptCompiler;

/**
 * 
 * CSS No Op Compiler Nothing needs to be compiled here
 * 
 * @author bpaulin
 * 
 */

@Component(label="CSS No Op Compiler Service", immediate = true)
@Service
public class CssNoOpCompilerImpl extends AbstractNoOpCompiler implements
        WebResourceScriptCompiler {
    @Override
    public String compiledScriptExtension() {
        return "css";
    }
}
