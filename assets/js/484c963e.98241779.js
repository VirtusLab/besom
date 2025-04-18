"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[901],{5644:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>p,contentTitle:()=>i,default:()=>m,frontMatter:()=>o,metadata:()=>s,toc:()=>l});var a=n(8168),r=(n(6540),n(5680));const o={title:"Inputs and Outputs"},i=void 0,s={unversionedId:"io",id:"io",title:"Inputs and Outputs",description:"Outputs are the primary asynchronous data structure of Pulumi programs.",source:"@site/docs/io.md",sourceDirName:".",slug:"/io",permalink:"/besom/docs/io",draft:!1,editUrl:"https://github.com/VirtusLab/besom/tree/main/website/docs/io.md",tags:[],version:"current",frontMatter:{title:"Inputs and Outputs"},sidebar:"docsSidebar",previous:{title:"Exports",permalink:"/besom/docs/exports"},next:{title:"Resource constructors",permalink:"/besom/docs/constructors"}},p={},l=[{value:"Outputs",id:"outputs",level:3},{value:"Inputs",id:"inputs",level:3}],u={toc:l},c="wrapper";function m(e){let{components:t,...n}=e;return(0,r.yg)(c,(0,a.A)({},u,n,{components:t,mdxType:"MDXLayout"}),(0,r.yg)("p",null,"Outputs are the ",(0,r.yg)("a",{parentName:"p",href:"/besom/docs/basics#inputs-and-outputs"},"primary asynchronous data structure of Pulumi")," programs. "),(0,r.yg)("h3",{id:"outputs"},"Outputs"),(0,r.yg)("p",null,"Outputs are:"),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("p",{parentName:"li"},"pure and lazy - meaning that they suspend evaluation of code until interpretation, which is perfomed by Besom\nruntime that runs ",(0,r.yg)("inlineCode",{parentName:"p"},"Pulumi.run")," function at the, so called, end-of-the-world. ")),(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("p",{parentName:"li"},"monadic - meaning that they expose ",(0,r.yg)("inlineCode",{parentName:"p"},"map")," and ",(0,r.yg)("inlineCode",{parentName:"p"},"flatMap")," operators and can be used in for-comprehensions"))),(0,r.yg)("p",null,"Outputs are capable of consuming other effects for which there exists an instance of ",(0,r.yg)("inlineCode",{parentName:"p"},"ToFuture")," typeclass. Besom\nprovides 3 such instances: "),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},"package ",(0,r.yg)("inlineCode",{parentName:"li"},"besom-core")," provides an instance for ",(0,r.yg)("inlineCode",{parentName:"li"},"scala.concurrent.Future")),(0,r.yg)("li",{parentName:"ul"},"package ",(0,r.yg)("inlineCode",{parentName:"li"},"besom-cats")," provides an instance for ",(0,r.yg)("inlineCode",{parentName:"li"},"cats.effect.IO")),(0,r.yg)("li",{parentName:"ul"},"package ",(0,r.yg)("inlineCode",{parentName:"li"},"besom-zio")," provides an instance for ",(0,r.yg)("inlineCode",{parentName:"li"},"zio.Task"))),(0,r.yg)("h3",{id:"inputs"},"Inputs"),(0,r.yg)("p",null,"Inputs are Besom types used wherever a value is expected to be provided by user primarily to ease the use of the\nconfiguration necessary for resource constructors to spawn infrastructure resources. Inputs allow user to provide both\nraw values, values that are wrapped in an ",(0,r.yg)("inlineCode",{parentName:"p"},"Output"),", both of the former or nothing at all when dealing with optional\nfields or even singular raw values or lists of values for fields that expect multiple values."),(0,r.yg)("p",null,"To make this more digestable - the basic ",(0,r.yg)("inlineCode",{parentName:"p"},"Input[A]")," type is declared as:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"opaque type Input[+A] >: A | Output[A] = A | Output[A]\n")),(0,r.yg)("p",null,"ane the ",(0,r.yg)("inlineCode",{parentName:"p"},"Input.Optional[A]")," variant is declared as:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"opaque type Optional[+A] >: Input[A | Option[A]] = Input[A | Option[A]]\n")),(0,r.yg)("p",null,"This allows for things like this:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"val int1: Input[Int] = 23\nval int2: Input[Int] = Output(23)\n// what if it's an optional value?\nval maybeInt1: Input.Optional[Int] = 23\nval maybeInt2: Input.Optional[Int] = None\nval maybeInt3: Input.Optional[Int] = Some(23)\n// yes, but also:\nval maybeInt4: Input.Optional[Int] = Output(23)\nval maybeInt5: Input.Optional[Int] = Output(Option(23))\nval maybeInt6: Input.Optional[Int] = Output(None)\n")),(0,r.yg)("p",null,"This elastic and permissive model was designed to allow a more declarative style and facilitate the implicit\nparallelism of evaluation. In fact, Outputs are meant to be thought of as short pipelines that one uses\nto transform properties and values obtained from one resource to be used as argument for another. If you're\nused to the classic way of working with monadic programs with chains of ",(0,r.yg)("inlineCode",{parentName:"p"},"flatMap")," and ",(0,r.yg)("inlineCode",{parentName:"p"},"map")," or for-comprehensions\nthis might seem a bit odd to you - why would we take values wrapped in Outputs as arguments? The answer is: previews!"),(0,r.yg)("p",null,"Outputs incorporate semantics of ",(0,r.yg)("inlineCode",{parentName:"p"},"Option")," to support Pulumi's preview / dry-run feature that allows one to see what\nchanges will be applied when the program is executed against the actual environment. This, however, means that Outputs\ncan short-circuit when a computed (provided by the engine) value is missing in dry-run and most properties on resources\nbelong to this category. It is entirely possible to structure a Besom program the same way one would structure a program\nthat uses Cats Effect IO or ZIO but once you ",(0,r.yg)("inlineCode",{parentName:"p"},"flatMap")," on an Output value that can be only obtained from actual environment\nshort-circuiting logic will kick in and all the subsequent ",(0,r.yg)("inlineCode",{parentName:"p"},"flatMap"),"/",(0,r.yg)("inlineCode",{parentName:"p"},"map")," steps will be skipped yielding a broken view\nof the changes that will get applied in your next change to the infrastructure. To avoid this problem it is highly\nrecommended to write Besom programs in a style highly reminiscent of direct style and use for-comprehensions only to transform\nproperties passed from configuration or declared resources to another resources. This way the graph of resources is fully\nknown in dry-run phase and can be properly inspected. Full power of monadic composition should be reserved for situations\nwhere it is strictly necessary."),(0,r.yg)("admonition",{type:"info"},(0,r.yg)("p",{parentName:"admonition"},"We are working on a solution that would allow us to track computed ",(0,r.yg)("inlineCode",{parentName:"p"},"Output")," values on the type level and therefore inform\nthe user (via a compile-time information or warning) that a dynamic subtree of resources will be spawned by their code\nthat won't be visible in preview.")))}m.isMDXComponent=!0},5680:(e,t,n)=>{n.d(t,{xA:()=>u,yg:()=>y});var a=n(6540);function r(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function o(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?o(Object(n),!0).forEach((function(t){r(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):o(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function s(e,t){if(null==e)return{};var n,a,r=function(e,t){if(null==e)return{};var n,a,r={},o=Object.keys(e);for(a=0;a<o.length;a++)n=o[a],t.indexOf(n)>=0||(r[n]=e[n]);return r}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(a=0;a<o.length;a++)n=o[a],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(r[n]=e[n])}return r}var p=a.createContext({}),l=function(e){var t=a.useContext(p),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},u=function(e){var t=l(e.components);return a.createElement(p.Provider,{value:t},e.children)},c="mdxType",m={inlineCode:"code",wrapper:function(e){var t=e.children;return a.createElement(a.Fragment,{},t)}},d=a.forwardRef((function(e,t){var n=e.components,r=e.mdxType,o=e.originalType,p=e.parentName,u=s(e,["components","mdxType","originalType","parentName"]),c=l(n),d=r,y=c["".concat(p,".").concat(d)]||c[d]||m[d]||o;return n?a.createElement(y,i(i({ref:t},u),{},{components:n})):a.createElement(y,i({ref:t},u))}));function y(e,t){var n=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var o=n.length,i=new Array(o);i[0]=d;var s={};for(var p in t)hasOwnProperty.call(t,p)&&(s[p]=t[p]);s.originalType=e,s[c]="string"==typeof e?e:r,i[1]=s;for(var l=2;l<o;l++)i[l]=n[l];return a.createElement.apply(null,i)}return a.createElement.apply(null,n)}d.displayName="MDXCreateElement"}}]);