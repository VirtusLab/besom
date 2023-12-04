"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[830],{3905:(e,t,a)=>{a.d(t,{Zo:()=>l,kt:()=>h});var n=a(7294);function r(e,t,a){return t in e?Object.defineProperty(e,t,{value:a,enumerable:!0,configurable:!0,writable:!0}):e[t]=a,e}function o(e,t){var a=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),a.push.apply(a,n)}return a}function s(e){for(var t=1;t<arguments.length;t++){var a=null!=arguments[t]?arguments[t]:{};t%2?o(Object(a),!0).forEach((function(t){r(e,t,a[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(a)):o(Object(a)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(a,t))}))}return e}function i(e,t){if(null==e)return{};var a,n,r=function(e,t){if(null==e)return{};var a,n,r={},o=Object.keys(e);for(n=0;n<o.length;n++)a=o[n],t.indexOf(a)>=0||(r[a]=e[a]);return r}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)a=o[n],t.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(e,a)&&(r[a]=e[a])}return r}var p=n.createContext({}),u=function(e){var t=n.useContext(p),a=t;return e&&(a="function"==typeof e?e(t):s(s({},t),e)),a},l=function(e){var t=u(e.components);return n.createElement(p.Provider,{value:t},e.children)},c="mdxType",m={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},d=n.forwardRef((function(e,t){var a=e.components,r=e.mdxType,o=e.originalType,p=e.parentName,l=i(e,["components","mdxType","originalType","parentName"]),c=u(a),d=r,h=c["".concat(p,".").concat(d)]||c[d]||m[d]||o;return a?n.createElement(h,s(s({ref:t},l),{},{components:a})):n.createElement(h,s({ref:t},l))}));function h(e,t){var a=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var o=a.length,s=new Array(o);s[0]=d;var i={};for(var p in t)hasOwnProperty.call(t,p)&&(i[p]=t[p]);i.originalType=e,i[c]="string"==typeof e?e:r,s[1]=i;for(var u=2;u<o;u++)s[u]=a[u];return n.createElement.apply(null,s)}return n.createElement.apply(null,a)}d.displayName="MDXCreateElement"},9263:(e,t,a)=>{a.d(t,{Z:()=>o});var n=a(7294),r=a(2263);const o=()=>{const{siteConfig:e}=(0,r.Z)(),{besomVersion:t}=e.customFields;return n.createElement(n.Fragment,null,t)}},7573:(e,t,a)=>{a.r(t),a.d(t,{assets:()=>p,contentTitle:()=>s,default:()=>m,frontMatter:()=>o,metadata:()=>i,toc:()=>u});var n=a(7462),r=(a(7294),a(3905));a(9263);const o={title:"Pulumi Basics"},s=void 0,i={unversionedId:"basics",id:"basics",title:"Pulumi Basics",description:"Before we dive into the details of Besom, let's take a look at the basics of Pulumi.",source:"@site/docs/basics.md",sourceDirName:".",slug:"/basics",permalink:"/besom/docs/basics",draft:!1,tags:[],version:"current",frontMatter:{title:"Pulumi Basics"},sidebar:"docsSidebar",previous:{title:"Tutorial",permalink:"/besom/docs/tutorial"},next:{title:"Overview",permalink:"/besom/docs/architecture"}},p={},u=[{value:"What is Pulumi?",id:"what-is-pulumi",level:3},{value:"What is Besom?",id:"what-is-besom",level:3},{value:"Concepts",id:"concepts",level:3},{value:"Projects",id:"projects",level:4},{value:"Programs",id:"programs",level:4},{value:"Stacks",id:"stacks",level:4},{value:"Stack information from code",id:"stack-information-from-code",level:5},{value:"Stack Outputs",id:"stack-outputs",level:5},{value:"Stack References",id:"stack-references",level:5},{value:"Resources",id:"resources",level:4},{value:"Inputs and Outputs",id:"inputs-and-outputs",level:4},{value:"Configuration and Secrets",id:"configuration-and-secrets",level:4},{value:"Accessing Configuration and Secrets from Code",id:"accessing-configuration-and-secrets-from-code",level:5},{value:"Providers",id:"providers",level:4},{value:"State",id:"state",level:4}],l={toc:u},c="wrapper";function m(e){let{components:t,...a}=e;return(0,r.kt)(c,(0,n.Z)({},l,a,{components:t,mdxType:"MDXLayout"}),(0,r.kt)("p",null,"Before we dive into the ",(0,r.kt)("a",{parentName:"p",href:"/besom/docs/architecture"},"details of Besom"),", let's take a look at the basics of Pulumi.\nThis page offers an executive summary of ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/"},"Pulumi's concepts"),"."),(0,r.kt)("h3",{id:"what-is-pulumi"},"What is Pulumi?"),(0,r.kt)("p",null,"Pulumi is a modern infrastructure as code platform. It leverages existing programming languages\nand their native ecosystem to interact with cloud resources through the Pulumi SDK."),(0,r.kt)("p",null,"Pulumi is a registered trademark of ",(0,r.kt)("a",{parentName:"p",href:"https://pulumi.com"},"Pulumi Corporation"),"."),(0,r.kt)("h3",{id:"what-is-besom"},"What is Besom?"),(0,r.kt)("p",null,"Besom is a ",(0,r.kt)("strong",{parentName:"p"},"Pulumi SDK for Scala 3"),". It allows you to use Scala to define your infrastructure\nin a type-safe and functional way."),(0,r.kt)("p",null,"Besom ",(0,r.kt)("strong",{parentName:"p"},"does NOT depend on Pulumi Java SDK"),", it is a completely separate implementation."),(0,r.kt)("admonition",{type:"caution"},(0,r.kt)("p",{parentName:"admonition"},"Please pay attention to your dependencies, ",(0,r.kt)("strong",{parentName:"p"},"only use ",(0,r.kt)("inlineCode",{parentName:"strong"},"org.virtuslab::besom-*"))," and not ",(0,r.kt)("inlineCode",{parentName:"p"},"com.pulumi:*"),".",(0,r.kt)("br",null))),(0,r.kt)("h3",{id:"concepts"},"Concepts"),(0,r.kt)("p",null,"It is important to understand the basic concepts of Pulumi before we dive into the details of Besom.\nWe strongly advise to get acquainted with ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/"},"Pulumi's concepts"),"\ndocumentation as all of that information applies to Besom as well."),(0,r.kt)("p",null,"Pulumi uses ",(0,r.kt)("a",{parentName:"p",href:"#programs"},"programs")," to define ",(0,r.kt)("a",{parentName:"p",href:"#resources"},"resources")," that are managed using ",(0,r.kt)("a",{parentName:"p",href:"#providers"},"providers"),"\nand result in ",(0,r.kt)("a",{parentName:"p",href:"#stacks"},"stacks"),"."),(0,r.kt)("p",null,"For more detailed information\nsee ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/how-pulumi-works/#how-pulumi-works"},"how Pulumi works"),"\ndocumentation section."),(0,r.kt)("h4",{id:"projects"},"Projects"),(0,r.kt)("p",null,"A ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/projects/"},"Pulumi project")," consists of:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},"a ",(0,r.kt)("a",{parentName:"li",href:"#programs"},"program")," that defines the desired infrastructure"),(0,r.kt)("li",{parentName:"ul"},"one or more ",(0,r.kt)("a",{parentName:"li",href:"#stacks"},"stack")," that defines the target environment for the program"),(0,r.kt)("li",{parentName:"ul"},"and metadata on how to run the program, such\nas ",(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/projects/#pulumi-yaml"},(0,r.kt)("inlineCode",{parentName:"a"},"Pulumi.yaml")),"\nand ",(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/projects/#stack-settings-file"},(0,r.kt)("inlineCode",{parentName:"a"},"Pulumi.<stackname>.yaml"))," files.")),(0,r.kt)("p",null,"You run the Pulumi CLI command ",(0,r.kt)("inlineCode",{parentName:"p"},"pulumi up")," from within your project directory to deploy your infrastructure."),(0,r.kt)("p",null,"Project source code is typically stored in a version control system such as Git.\nIn addition to the project source code, Pulumi also stores a snapshot of the project state in the ",(0,r.kt)("a",{parentName:"p",href:"#state"},"backend"),"."),(0,r.kt)("p",null,"Besom projects are no different. You can use the same project structure and workflow as you would with other Pulumi\nSDKs.\nThe only difference is that you use ",(0,r.kt)("inlineCode",{parentName:"p"},"runtime: scala")," in\nyour ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/projects/project-file/"},(0,r.kt)("inlineCode",{parentName:"a"},"Pulumi.yaml")),"\nwith ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/projects/project-file/#runtime-options"},"runtime options")," being:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"binary")," - a path to pre-built executable JAR"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"use-executor")," - force a specific executor path instead of probing the project directory and ",(0,r.kt)("inlineCode",{parentName:"li"},"PATH"))),(0,r.kt)("p",null,"A minimal Besom ",(0,r.kt)("inlineCode",{parentName:"p"},"Pulumi.yaml")," project file:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-yaml"},"name: Example Besom project file with only required attributes\nruntime: scala\n")),(0,r.kt)("h4",{id:"programs"},"Programs"),(0,r.kt)("p",null,"A Pulumi program, written in a general-purpose programming language, is a collection of ",(0,r.kt)("a",{parentName:"p",href:"#resources"},"resources"),"\nthat are deployed to a ",(0,r.kt)("a",{parentName:"p",href:"#stacks"},"stack"),"."),(0,r.kt)("p",null,"A minimal Besom program consists of:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"project.scala")," - the program dependencies (here we\nuse ",(0,r.kt)("a",{parentName:"li",href:"https://scala-cli.virtuslab.org/docs/guides/using-directives/"},"Scala-CLI directives"),")",(0,r.kt)("pre",{parentName:"li"},(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'//> using scala "3.3.1"\n//> using plugin "org.virtuslab::besom-compiler-plugin:0.1.0"\n//> using dep "org.virtuslab::besom-core:0.1.0"\n'))),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"Main.scala")," - the actual program written in Scala",(0,r.kt)("pre",{parentName:"li"},(0,r.kt)("code",{parentName:"pre",className:"language-scala"},"import besom.*\n\n@main def main = Pulumi.run {\n  for \n    _ <- log.warn(\"Nothing's here yet, it's waiting for you to write some code!\")\n  yield exports()\n}\n")))),(0,r.kt)("admonition",{type:"tip"},(0,r.kt)("p",{parentName:"admonition"},"Pass ",(0,r.kt)("a",{parentName:"p",href:"/besom/docs/context"},(0,r.kt)("inlineCode",{parentName:"a"},"Context"))," everywhere you are using Besom outside of ",(0,r.kt)("inlineCode",{parentName:"p"},"Pulumi.run")," block with ",(0,r.kt)("inlineCode",{parentName:"p"},"(using besom.Context)"),".")),(0,r.kt)("h4",{id:"stacks"},"Stacks"),(0,r.kt)("p",null,(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/stack/"},"Pulumi stack")," is a separate, isolated, independently configurable\ninstance of a Pulumi ",(0,r.kt)("a",{parentName:"p",href:"#programs"},"program"),", and can be updated and referred to independently.\nA ",(0,r.kt)("a",{parentName:"p",href:"#projects"},"project")," can have as many stacks as needed."),(0,r.kt)("p",null,(0,r.kt)("a",{parentName:"p",href:"#projects"},"Projects")," and ",(0,r.kt)("a",{parentName:"p",href:"#stacks"},"stacks")," are intentionally flexible so that they can accommodate diverse needs\nacross a spectrum of team, application, and infrastructure scenarios. Learn more about organizing your code in\n",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/using-pulumi/organizing-projects-stacks"},"Pulumi projects and stacks")," documentation."),(0,r.kt)("p",null,"By default, Pulumi creates a stack for you when you start a new project using the ",(0,r.kt)("inlineCode",{parentName:"p"},"pulumi new")," command.\nEach stack that is created in a project will have a file\nnamed ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/projects/#stack-settings-file"},(0,r.kt)("inlineCode",{parentName:"a"},"Pulumi.<stackname>.yaml")),"\nin the root of the ",(0,r.kt)("a",{parentName:"p",href:"#projects"},"project")," directory that contains the ",(0,r.kt)("a",{parentName:"p",href:"#configuration-and-secrets"},"configuration")," specific to this\nstack."),(0,r.kt)("admonition",{type:"tip"},(0,r.kt)("p",{parentName:"admonition"},"The recommended practice is to ",(0,r.kt)("strong",{parentName:"p"},"check stack files into source control")," as a means of collaboration.",(0,r.kt)("br",null),"\nSince secret values are encrypted, it is safe to check in these stack settings.")),(0,r.kt)("h5",{id:"stack-information-from-code"},"Stack information from code"),(0,r.kt)("p",null,"You can access stack information from your ",(0,r.kt)("a",{parentName:"p",href:"#programs"},"program")," context using the ",(0,r.kt)("inlineCode",{parentName:"p"},"urn")," method, e.g.:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},"urn.map(_.stack) // the stack name\nurn.map(_.project) // the stack name\n")),(0,r.kt)("h5",{id:"stack-outputs"},"Stack Outputs"),(0,r.kt)("p",null,"Stacks can export values as ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/stack/#outputs"},"Stack Outputs"),".\nThese outputs are shown by Pulumi CLI commands, and are displayed in the Pulumi Cloud, and can be accessed\nprogrammatically using ",(0,r.kt)("a",{parentName:"p",href:"#stack-references"},"Stack References"),"."),(0,r.kt)("p",null,"To export values from a stack in Besom, use the ",(0,r.kt)("a",{parentName:"p",href:"/besom/docs/exports"},(0,r.kt)("inlineCode",{parentName:"a"},"Pulumi.exports"))," function in your program."),(0,r.kt)("h5",{id:"stack-references"},"Stack References"),(0,r.kt)("p",null,(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/stack/#stackreferences"},"Stack References")," allow you to use outputs from other\n",(0,r.kt)("a",{parentName:"p",href:"#stacks"},"stacks")," in your ",(0,r.kt)("a",{parentName:"p",href:"#programs"},"program"),"."),(0,r.kt)("p",null,"To reference values from another stack, create an instance of the ",(0,r.kt)("inlineCode",{parentName:"p"},"StackReference")," type using the fully qualified\nname of the stack as an input, and then read exported stack outputs by their name."),(0,r.kt)("p",null,(0,r.kt)("inlineCode",{parentName:"p"},"StackReference")," is not implemented yet in Besom, coming soon."),(0,r.kt)("h4",{id:"resources"},"Resources"),(0,r.kt)("p",null,"Resources are the primary ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/resources/"},"construct of Pulumi")," programs.\nResources represent the fundamental units that make up your infrastructure, such as a compute instance,\na storage bucket, or a Kubernetes cluster."),(0,r.kt)("p",null,"Resources are defined using a ",(0,r.kt)("a",{parentName:"p",href:"/besom/docs/constructors"},(0,r.kt)("strong",{parentName:"a"},"resource constructor")),". Each resource in Pulumi has:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},"a ",(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/resources/names/#resource-names"},"logical name and a physical name"),"\nThe logical name establishes a notion of identity within Pulumi, and the physical name is used as identity by the\nprovider"),(0,r.kt)("li",{parentName:"ul"},"a ",(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/resources/names/#types"},"resource type"),", which identifies the provider and the\nkind of resource being created"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/resources/names/#types"},"Pulumi URN"),", which is an automatically constructed\nglobally unique identifier for the resource.")),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'val redisNamespace = Namespace(s"redis-cluster-namespace-$name")\nredisNamespace.id // the physical name\nredisNamespace.urn // the globally unique identifier\nredisNamespace.urn.map(_.resourceName) // the logical name\nredisNamespace.urn.map(_.resourceType) // the resource type\n')),(0,r.kt)("p",null,"Each resource can also have:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},"a set of ",(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/resources/properties/"},"arguments")," that define the behavior of the\nresulting infrastructure"),(0,r.kt)("li",{parentName:"ul"},"and a set of ",(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/options/"},"options")," that control how the resource is created and\nmanaged by the Pulumi engine.")),(0,r.kt)("p",null,"Every ",(0,r.kt)("a",{parentName:"p",href:"#resources"},"resource")," is managed by a ",(0,r.kt)("a",{parentName:"p",href:"#providers"},"provider")," which is a plugin that\nprovides the implementation details. If not specified explicitly, the default provider is used.\nProviders can be configured\nusing ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/resources/providers/#explicit-provider-configuration"},"provider configuration"),"."),(0,r.kt)("h4",{id:"inputs-and-outputs"},"Inputs and Outputs"),(0,r.kt)("p",null,"Inputs and Outputs are the\nprimary ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/inputs-outputs/"},"asynchronous data types in Pulumi"),",\nand they signify values that will be provided by the engine later, when the resource is created and its properties can\nbe fetched.\n",(0,r.kt)("inlineCode",{parentName:"p"},"Input[A]")," type is an alias for ",(0,r.kt)("inlineCode",{parentName:"p"},"Output[A]")," type used by ",(0,r.kt)("a",{parentName:"p",href:"#resources"},"resource")," arguments."),(0,r.kt)("p",null,"Outputs are values of type ",(0,r.kt)("inlineCode",{parentName:"p"},"Output[A]")," and behave very much\nlike ",(0,r.kt)("a",{parentName:"p",href:"https://en.wikipedia.org/wiki/Monad_(functional_programming)"},"monads"),".\nThis is necessary because output values are not fully known until the infrastructure resource has actually completed\nprovisioning, which happens asynchronously after the program has finished executing."),(0,r.kt)("p",null,"Outputs are used to:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},"automatically captures dependencies between ",(0,r.kt)("a",{parentName:"li",href:"#resources"},"resources")),(0,r.kt)("li",{parentName:"ul"},"provide a way to express transformations on its value before it's known"),(0,r.kt)("li",{parentName:"ul"},"deffer the evaluation of its value until it's known"),(0,r.kt)("li",{parentName:"ul"},"track the ",(0,r.kt)("em",{parentName:"li"},"secretness")," of its value")),(0,r.kt)("p",null,"Output transformations available in Besom:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"/besom/docs/apply_methods"},(0,r.kt)("inlineCode",{parentName:"a"},"map")," and ",(0,r.kt)("inlineCode",{parentName:"a"},"flatMap"))," methods take a callback that receives the plain value, and computes a new\noutput"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"/besom/docs/lifting"},"lifting")," directly read properties off an output value"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"/besom/docs/interpolator"},"interpolation")," concatenate string outputs with other strings directly"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"sequence")," method combines multiple outputs into a single output of a list"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"zip")," method combines multiple outputs into a single output of a tuple"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"traverse")," method transforms a map of outputs into a single output of a map")),(0,r.kt)("p",null,"To create an output from a plain value, use the ",(0,r.kt)("inlineCode",{parentName:"p"},"Output")," constructor, e.g.:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'val hello = Output("hello")\nval world = Output.secret("world")\n')),(0,r.kt)("p",null,"To transform an output value, use the ",(0,r.kt)("inlineCode",{parentName:"p"},"map")," and ",(0,r.kt)("inlineCode",{parentName:"p"},"flatMap")," methods, e.g.:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'val hello = Output("hello").map(_.toUpperCase)\nval world = Output.secret("world")\nval helloWorld: Output[String] = hello.flatMap(h => h + "_" + world.map(_.toUpperCase))\n')),(0,r.kt)("p",null,"If you have multiple outputs of the same type and need to use them together ",(0,r.kt)("strong",{parentName:"p"},"as a list")," you can use\n",(0,r.kt)("inlineCode",{parentName:"p"},"Output.sequence")," method to combine them into a single output:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},"val port: Output[String] = pod.name\nval host: Output[String] = node.hostname\nval hello: Output[List[String]] = List(host, port).sequence // we use the extension method here\n")),(0,r.kt)("p",null,"If you have multiple outputs of different types and need to use them together ",(0,r.kt)("strong",{parentName:"p"},"as a tuple")," you can use the standard\n",(0,r.kt)("a",{parentName:"p",href:"https://scala-lang.org/api/3.x/scala/collection/View.html#zip-1dd"},(0,r.kt)("inlineCode",{parentName:"a"},"zip"))," method and pattern matching (",(0,r.kt)("inlineCode",{parentName:"p"},"case"),") to\ncombine them into a single output:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'val port: Output[Int] = pod.port\nval host: Output[String] = node.hostname\nval hello: Output[(String, Int)] = host.zip(port)\nval url: Output[String] = hello.map { case (a, b) => s"https://$hostname:$port/" }\n')),(0,r.kt)("p",null,"If you have a map of outputs and need to use them together ",(0,r.kt)("strong",{parentName:"p"},"as a map")," you can use\n",(0,r.kt)("inlineCode",{parentName:"p"},"Output.traverse")," method to combine them into a single output:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},"val m: Map[String, Output[String]] = Map(pod.name -> pod.port)\nval o: Output[Map[String, String]] = m.traverse // we use the extension method here\n")),(0,r.kt)("p",null,"You can also use ",(0,r.kt)("inlineCode",{parentName:"p"},"Output.traverse")," like that:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'val names: List[String] = List("John", "Paul")\nval outputNames: Output[List[String]] = names.traverse(name => Output(name))\n')),(0,r.kt)("p",null,"To access ",(0,r.kt)("inlineCode",{parentName:"p"},"String")," outputs directly, use the ",(0,r.kt)("a",{parentName:"p",href:"/besom/docs/interpolator"},"interpolator"),":"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'val port: Output[Int] = pod.port\nval host: Output[String] = node.hostname\nval https: Output[String] = p"https://$host:$port/api/"\n')),(0,r.kt)("p",null,"We encourage you to learn more about relationship between ",(0,r.kt)("a",{parentName:"p",href:"#resources"},"resources")," and ",(0,r.kt)("a",{parentName:"p",href:"#inputs-and-outputs"},"outputs"),"\nin the ",(0,r.kt)("a",{parentName:"p",href:"/besom/docs/constructors"},"Resource constructors and asynchronicity")," section."),(0,r.kt)("h4",{id:"configuration-and-secrets"},"Configuration and Secrets"),(0,r.kt)("p",null,(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/config"},"Configuration")," or ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/secrets/"},"Secret"),"\nis a set of key-value pairs that influence the behavior of a Pulumi program."),(0,r.kt)("p",null,"Configuration or secret keys use the format ",(0,r.kt)("inlineCode",{parentName:"p"},"[<namespace>:]<key-name>"),", with a colon delimiting the optional namespace\nand the actual key name. Pulumi automatically uses the current project name\nfrom ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/projects/#pulumi-yaml"},(0,r.kt)("inlineCode",{parentName:"a"},"Pulumi.yaml"))," as the default key namespace."),(0,r.kt)("p",null,"Configuration values can be set in two ways:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/projects/#stack-settings-file"},(0,r.kt)("inlineCode",{parentName:"a"},"Pulumi.<stackname>.yaml"))," file"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/config/#setting-and-getting-configuration-values"},(0,r.kt)("inlineCode",{parentName:"a"},"pulumi config set")),"\nand ",(0,r.kt)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/secrets/#secrets"},(0,r.kt)("inlineCode",{parentName:"a"},"pulumi config set --secret"))," commands")),(0,r.kt)("h5",{id:"accessing-configuration-and-secrets-from-code"},"Accessing Configuration and Secrets from Code"),(0,r.kt)("p",null,"Configuration and secret values can be ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/config/#code"},"accessed"),"\nfrom ",(0,r.kt)("a",{parentName:"p",href:"#programs"},"programs"),"\nusing the ",(0,r.kt)("inlineCode",{parentName:"p"},"Config")," object, e.g.:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'val a = config.get("aws:region")\nval b = Config("aws").map(_.get("region"))\n')),(0,r.kt)("p",null,"If the configuration value is a secret, it will be marked internally as such and redacted in console outputs."),(0,r.kt)("admonition",{type:"note"},(0,r.kt)("p",{parentName:"admonition"},"Secret values are\nautomatically ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/secrets/#configuring-secrets-encryption"},"encrypted and stored")," in\nthe Pulumi ",(0,r.kt)("a",{parentName:"p",href:"#state"},"state"),".")),(0,r.kt)("admonition",{type:"tip"},(0,r.kt)("p",{parentName:"admonition"},"Secrets in Besom differ in behavior from other Pulumi SDKs. In other SDKs, if you try to get a config key that is a\nsecret, you will obtain it as plaintext (and due to ",(0,r.kt)("a",{parentName:"p",href:"https://github.com/pulumi/pulumi/issues/7127"},"a bug")," you won't even\nget a\nwarning)."),(0,r.kt)("p",{parentName:"admonition"},"We choose to do the right thing in Besom and ",(0,r.kt)("strong",{parentName:"p"},"return all configs as Outputs")," so that we can handle failure in pure,\nfunctional way, and ",(0,r.kt)("strong",{parentName:"p"},"automatically")," mark secret values\n",(0,r.kt)("strong",{parentName:"p"},"as ",(0,r.kt)("a",{parentName:"strong",href:"https://www.pulumi.com/docs/concepts/secrets/#how-secrets-relate-to-outputs"},"secret Outputs")),".")),(0,r.kt)("h4",{id:"providers"},"Providers"),(0,r.kt)("p",null,"A ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/resources/providers/"},"resource provider")," is a plugin that\nhandles communications with a cloud service to create, read, update, and delete the resources you define in\nyour Pulumi ",(0,r.kt)("a",{parentName:"p",href:"#programs"},"programs"),"."),(0,r.kt)("p",null,"You import a Provider SDK (e.g. ",(0,r.kt)("inlineCode",{parentName:"p"},"import besom.api.aws"),") library in you ",(0,r.kt)("a",{parentName:"p",href:"#programs"},"program"),", Pulumi passes your code to\nthe language host plugin (i.e. ",(0,r.kt)("inlineCode",{parentName:"p"},"pulumi-language-scala"),"), waits to be notified of resource registrations, assembles\na model of your desired ",(0,r.kt)("a",{parentName:"p",href:"#state"},"state"),", and calls on the resource provider (e.g. ",(0,r.kt)("inlineCode",{parentName:"p"},"pulumi-resource-aws"),") to produce that\nstate.\nThe resource provider translates those requests into API calls to the cloud service or platform."),(0,r.kt)("p",null,"Providers can be configured\nusing ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/resources/providers/#explicit-provider-configuration"},"provider configuration"),"."),(0,r.kt)("admonition",{type:"tip"},(0,r.kt)("p",{parentName:"admonition"},"It is recommended to ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/blog/disable-default-providers/"},"disable default providers"),"\nif not\nfor ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/config#pulumi-configuration-options"},"all providers, at least for Kubernetes"),".")),(0,r.kt)("h4",{id:"state"},"State"),(0,r.kt)("p",null,"State is a snapshot of your ",(0,r.kt)("a",{parentName:"p",href:"#projects"},"project")," ",(0,r.kt)("a",{parentName:"p",href:"#resources"},"resources")," that is stored in\na ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/state/#deciding-on-a-state-backend"},"backend"),"\nwith ",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/intro/cloud-providers/pulumi-service/"},"Pulumi Service")," being the default."),(0,r.kt)("p",null,"State is used to:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},"track ",(0,r.kt)("a",{parentName:"li",href:"#resources"},"resources")," that are created by your ",(0,r.kt)("a",{parentName:"li",href:"#programs"},"program")),(0,r.kt)("li",{parentName:"ul"},"record the relationship between resources"),(0,r.kt)("li",{parentName:"ul"},"store metadata about your ",(0,r.kt)("a",{parentName:"li",href:"#projects"},"project")," and ",(0,r.kt)("a",{parentName:"li",href:"#stacks"},"stacks")),(0,r.kt)("li",{parentName:"ul"},"and store ",(0,r.kt)("a",{parentName:"li",href:"#configuration-and-secrets"},"configuration")," and secret values"),(0,r.kt)("li",{parentName:"ul"},"and store ",(0,r.kt)("a",{parentName:"li",href:"#stack-outputs"},"stack outputs"))),(0,r.kt)("admonition",{type:"note"},(0,r.kt)("p",{parentName:"admonition"},"Fore extra\ncurious ",(0,r.kt)("a",{parentName:"p",href:"https://pulumi-developer-docs.readthedocs.io/en/latest/architecture/deployment-schema.html"},"here's the internal state schema"))))}m.isMDXComponent=!0}}]);