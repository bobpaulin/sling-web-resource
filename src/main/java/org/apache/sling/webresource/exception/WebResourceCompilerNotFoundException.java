package org.apache.sling.webresource.exception;

/**
 * 
 * Exception for not finding Web Resource Compiler for given resource
 * 
 * @author bpaulin
 * 
 */
public class WebResourceCompilerNotFoundException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = -4792982490718998543L;

	public WebResourceCompilerNotFoundException() {
        super();
    }

    public WebResourceCompilerNotFoundException(Throwable e) {
        super(e);
    }

    public WebResourceCompilerNotFoundException(String message) {
        super(message);
    }

    public WebResourceCompilerNotFoundException(String message, Throwable e) {
        super(message, e);
    }
}
