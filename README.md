#Sling Web Resource

This project provides an abstraction that allows pluggable web resource compilers
 such as CoffeeScript to compile within Sling.

This project includes just the framework which includes a JavaScript Runtime and features to maintain the a cache of the compiled scripts.  
The examples below require the inclusion of the sling-coffee-core bundle at [sling-coffee](https://github.com/bobpaulin/sling-coffee) project.

Compilers should extend the WebResourceScriptCompiler interface.

## Web Resource Groups

### Basic Usage
To use a web resource group web resource files must be placed under a node with the webresource:WebResourceGroup Mixin
This mix in requires the user to define a webresource:name property
	
So for example
	-content
		-chaplin (webresource:WebResourceGroup)
			- chaplin.coffee

Or if you're using a json based content uploader the chaplin folder would have the following in the chaplin.json file

	{
		"jcr:primaryType": "nt:folder",
		"jcr:mixinTypes": ["webresource:WebResourceGroup"],
		"webresource:name": "chaplin"
	}

The group can then be called using the web resource tag as follows:
    <webresource:webresource groupName="chaplin"/>

#### Cache Path
Optionally you can specify where the web resources will be conpiled to by adding a webresource:cachePath property.

For instance in the example above where /content/chaplin is the web resource folder and the following configuration is used

    {
        "jcr:primaryType": "nt:folder",
        "jcr:mixinTypes": ["webresource:WebResourceGroup"],
        "webresource:name": "chaplin",
        "webresource:cachePath": "/content/chaplin/js"
    }

Then the chaplin.coffee file will be compiled to /content/chaplin/js/chaplin.js

#### Compiler Options
In some cases options need to be passed to the compilers of some web resources.  For example in CoffeeScript compiled files
will be wrapped in a self executing function.  This behavior can be disabled by specifying the { bare: true } compile option.
These compile options can be set at the web resource group level by adding the following properties:

	{
		"jcr:primaryType": "nt:folder",
		"jcr:mixinTypes": ["webresource:WebResourceGroup"],
		"webresource:name": "chaplin",
		"webresource:cachePath": "/content/chaplin/js",
		"compileOptions": {
			"jcr:primaryType": "webresource:CompileOptions",
			"compileOption":{
				"jcr:primaryType": "webresource:CompileOption",
				"webresource:compiler": "coffeescript",
				"webresource:compileOptionName": "bare",
				"webresource:compileOptionValue": "true"
			
			}
		}
	}

The coffeescript compiler reads compile options with the webresource:compiler set to coffeescript.  Zero to many compile options 
may be set on a web resource group node.

### Web Resource Tag

The Web Resource Tag provides a convenience method for inserting web resources into JSPs.  The tag comes with several options

#### Inline

At times it's beneficial to inline the script directly into the page instead of making another http request to retrieve it.  
This can be accomplished by using the inline option

    <webresource:webresource groupName="chaplin" inline="true"/>

# License

This project is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).