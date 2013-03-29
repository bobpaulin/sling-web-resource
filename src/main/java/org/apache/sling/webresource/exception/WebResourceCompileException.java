package org.apache.sling.webresource.exception;

/**
 * 
 * Exception for Web Resource compilation
 * 
 * @author bpaulin
 *
 */
public class WebResourceCompileException extends Exception {
    
    public WebResourceCompileException() {
        super();
    }
    
    public WebResourceCompileException(Throwable e)
    {
        super(e);
    }
    
    public WebResourceCompileException(String message)
    {
        super(message);
    }
    
    public WebResourceCompileException(String message, Throwable e)
    {
        super(message, e);
    }
}
