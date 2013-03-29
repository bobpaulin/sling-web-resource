Sling Web Resource

This project provides an abstraction that allows pluggable web resource compilers
 such as CoffeeScript to compile within Sling.

Compilers should extend the WebResourceScriptCompiler interface then they may use the WebResourceTag as follows:

Just include the following tag library in you JSP
<%@taglib prefix="webresource" uri="http://sling.apache.org/taglibs/sling/webresource/1.0.0"%>

Then add the tag to compile a Web Resource file (such as CoffeeScript) from a specific path

<webresource:webresource path="/content/coffee/demo/demo.coffee" wrapWithTag="script"/>

# License

This project is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).