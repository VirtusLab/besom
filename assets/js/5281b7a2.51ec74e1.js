"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[927],{3905:(e,t,n)=>{n.d(t,{Zo:()=>c,kt:()=>h});var a=n(7294);function r(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function i(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function o(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?i(Object(n),!0).forEach((function(t){r(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):i(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function l(e,t){if(null==e)return{};var n,a,r=function(e,t){if(null==e)return{};var n,a,r={},i=Object.keys(e);for(a=0;a<i.length;a++)n=i[a],t.indexOf(n)>=0||(r[n]=e[n]);return r}(e,t);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(a=0;a<i.length;a++)n=i[a],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(r[n]=e[n])}return r}var s=a.createContext({}),p=function(e){var t=a.useContext(s),n=t;return e&&(n="function"==typeof e?e(t):o(o({},t),e)),n},c=function(e){var t=p(e.components);return a.createElement(s.Provider,{value:t},e.children)},u="mdxType",m={inlineCode:"code",wrapper:function(e){var t=e.children;return a.createElement(a.Fragment,{},t)}},d=a.forwardRef((function(e,t){var n=e.components,r=e.mdxType,i=e.originalType,s=e.parentName,c=l(e,["components","mdxType","originalType","parentName"]),u=p(n),d=r,h=u["".concat(s,".").concat(d)]||u[d]||m[d]||i;return n?a.createElement(h,o(o({ref:t},c),{},{components:n})):a.createElement(h,o({ref:t},c))}));function h(e,t){var n=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var i=n.length,o=new Array(i);o[0]=d;var l={};for(var s in t)hasOwnProperty.call(t,s)&&(l[s]=t[s]);l.originalType=e,l[u]="string"==typeof e?e:r,o[1]=l;for(var p=2;p<i;p++)o[p]=n[p];return a.createElement.apply(null,o)}return a.createElement.apply(null,n)}d.displayName="MDXCreateElement"},1527:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>s,contentTitle:()=>o,default:()=>m,frontMatter:()=>i,metadata:()=>l,toc:()=>p});var a=n(7462),r=(n(7294),n(3905));const i={title:"Overview"},o=void 0,l={unversionedId:"architecture",id:"architecture",title:"Overview",description:"Pulumi runtime is asynchronous by design. The goal is to allow the user's program to declare all the necessary resources",source:"@site/docs/architecture.md",sourceDirName:".",slug:"/architecture",permalink:"/besom/docs/architecture",draft:!1,tags:[],version:"current",frontMatter:{title:"Overview"},sidebar:"docsSidebar",previous:{title:"Pulumi Basics",permalink:"/besom/docs/basics"},next:{title:"Context and imports",permalink:"/besom/docs/context"}},s={},p=[],c={toc:p},u="wrapper";function m(e){let{components:t,...n}=e;return(0,r.kt)(u,(0,a.Z)({},c,n,{components:t,mdxType:"MDXLayout"}),(0,r.kt)("p",null,"Pulumi runtime is ",(0,r.kt)("strong",{parentName:"p"},"asynchronous by design"),". The goal is to allow the user's program to declare all the necessary resources\nas fast as possible so that Pulumi engine can make informed decisions about which parts of the deployment plan can be\nexecuted in parallel and therefore yield good performance. "),(0,r.kt)("p",null,"Each of the Pulumi SDKs reflects this reality by leveraging the language's asynchronous datatype to implement\nthe internals of the SDK that communicate with Pulumi engine via gRPC.\nFor Python, it's ",(0,r.kt)("inlineCode",{parentName:"p"},"asyncio"),", for JavaScript and TypeScript it's ",(0,r.kt)("inlineCode",{parentName:"p"},"Promise"),",\nfor C# it's ",(0,r.kt)("inlineCode",{parentName:"p"},"Task")," and for Java it's ",(0,r.kt)("inlineCode",{parentName:"p"},"CompletableFuture"),". "),(0,r.kt)("p",null,"Scala is a ",(0,r.kt)("strong",{parentName:"p"},"bit different")," in this regard. Due to extraordinary amount of innovation happening in the community and the\noverall focus on concurrency and asynchronicity Scala now has 3 main asynchronous, concurrent data types:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},"standard library's ",(0,r.kt)("inlineCode",{parentName:"li"},"Future"),", which is used heavily in Akka / Pekko ecosystems, "),(0,r.kt)("li",{parentName:"ul"},"cats-effect ",(0,r.kt)("inlineCode",{parentName:"li"},"IO")," used extensively by the cats ecosystem "),(0,r.kt)("li",{parentName:"ul"},"and ",(0,r.kt)("inlineCode",{parentName:"li"},"ZIO")," that also has its own ecosystem. ")),(0,r.kt)("p",null,"The last two of these data types are ",(0,r.kt)("a",{parentName:"p",href:"https://en.wikipedia.org/wiki/Lazy_evaluation"},"lazily evaluated"),". "),(0,r.kt)("p",null,"To support and integrate them with Besom a decision was made to encode the SDK using the same ",(0,r.kt)("strong",{parentName:"p"},"lazy and pure semantics"),"\nof execution that leverage the preferred datatype of the user. While this architectural choice has little impact on what\ncan be done currently in standalone Pulumi programs, in the future we are going to support Pulumi's Automation API\nwhich allows users to directly embed Besom into their applications.\nIt is at that point when direct integration with all 3 technological stacks will be the most meaningful.\n\u200b"),(0,r.kt)("p",null,"Besom stands alone in this choice and due to it ",(0,r.kt)("strong",{parentName:"p"},"has some differences")," in comparison to how other Pulumi SDKs operate. "),(0,r.kt)("p",null,"Following sections explain and showcase said differences:"),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"constructors"},"Resource constructors")," - resource constructors are pure functions that return Outputs"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"context"},"Context")," - context is passed around implicitly via Scala's Context Function"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"exports"},"Exports")," - your program is a function that returns Stack Outputs"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"laziness"},"Laziness")," - dangling resources are possible and resource constructors are memoized"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"apply_methods"},"Apply method")," - use ",(0,r.kt)("inlineCode",{parentName:"li"},"map")," and ",(0,r.kt)("inlineCode",{parentName:"li"},"flatMap")," to compose Outputs, not ",(0,r.kt)("inlineCode",{parentName:"li"},"apply")),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"logging"},"Logging")," - all logging statements need to be composed into the main flow"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"lifting"},"Lifting")," - first class support of lifting via Scala 3 extension methods"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"interpolator"},"String interpolation")," - use ",(0,r.kt)("inlineCode",{parentName:"li"},"p")," type-safe string interpolator"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"components"},"Components")," - use ",(0,r.kt)("inlineCode",{parentName:"li"},"case class")," to define components"),(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("a",{parentName:"li",href:"compiler_plugin"},"Compiler plugin")," - use the compiler plugin to catch mistakes early and avoid common pitfalls")))}m.isMDXComponent=!0}}]);