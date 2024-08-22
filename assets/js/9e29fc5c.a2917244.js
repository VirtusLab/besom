"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[612],{5680:(e,t,r)=>{r.d(t,{xA:()=>s,yg:()=>d});var n=r(6540);function i(e,t,r){return t in e?Object.defineProperty(e,t,{value:r,enumerable:!0,configurable:!0,writable:!0}):e[t]=r,e}function o(e,t){var r=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),r.push.apply(r,n)}return r}function a(e){for(var t=1;t<arguments.length;t++){var r=null!=arguments[t]?arguments[t]:{};t%2?o(Object(r),!0).forEach((function(t){i(e,t,r[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(r)):o(Object(r)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(r,t))}))}return e}function l(e,t){if(null==e)return{};var r,n,i=function(e,t){if(null==e)return{};var r,n,i={},o=Object.keys(e);for(n=0;n<o.length;n++)r=o[n],t.indexOf(r)>=0||(i[r]=e[r]);return i}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)r=o[n],t.indexOf(r)>=0||Object.prototype.propertyIsEnumerable.call(e,r)&&(i[r]=e[r])}return i}var p=n.createContext({}),c=function(e){var t=n.useContext(p),r=t;return e&&(r="function"==typeof e?e(t):a(a({},t),e)),r},s=function(e){var t=c(e.components);return n.createElement(p.Provider,{value:t},e.children)},u="mdxType",m={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},g=n.forwardRef((function(e,t){var r=e.components,i=e.mdxType,o=e.originalType,p=e.parentName,s=l(e,["components","mdxType","originalType","parentName"]),u=c(r),g=i,d=u["".concat(p,".").concat(g)]||u[g]||m[g]||o;return r?n.createElement(d,a(a({ref:t},s),{},{components:r})):n.createElement(d,a({ref:t},s))}));function d(e,t){var r=arguments,i=t&&t.mdxType;if("string"==typeof e||i){var o=r.length,a=new Array(o);a[0]=g;var l={};for(var p in t)hasOwnProperty.call(t,p)&&(l[p]=t[p]);l.originalType=e,l[u]="string"==typeof e?e:i,a[1]=l;for(var c=2;c<o;c++)a[c]=r[c];return n.createElement.apply(null,a)}return n.createElement.apply(null,r)}g.displayName="MDXCreateElement"},155:(e,t,r)=>{r.r(t),r.d(t,{assets:()=>p,contentTitle:()=>a,default:()=>m,frontMatter:()=>o,metadata:()=>l,toc:()=>c});var n=r(8168),i=(r(6540),r(5680));const o={title:"Compiler plugin"},a=void 0,l={unversionedId:"compiler_plugin",id:"compiler_plugin",title:"Compiler plugin",description:"Besom compiler plugin is a tool that helps to avoid common mistakes when writing Pulumi programs in Scala.",source:"@site/docs/compiler_plugin.md",sourceDirName:".",slug:"/compiler_plugin",permalink:"/besom/docs/compiler_plugin",draft:!1,editUrl:"https://github.com/VirtusLab/besom/tree/main/website/docs/compiler_plugin.md",tags:[],version:"current",frontMatter:{title:"Compiler plugin"},sidebar:"docsSidebar",previous:{title:"JSON API",permalink:"/besom/docs/json"},next:{title:"Missing features",permalink:"/besom/docs/missing"}},p={},c=[],s={toc:c},u="wrapper";function m(e){let{components:t,...r}=e;return(0,i.yg)(u,(0,n.A)({},s,r,{components:t,mdxType:"MDXLayout"}),(0,i.yg)("p",null,"Besom compiler plugin is a tool that helps to avoid common mistakes when writing Pulumi programs in Scala.\nIt is recommended to use it in all Pulumi programs written in Scala."),(0,i.yg)("p",null,"Currently, the plugin provides the following features:"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"It makes it a compile error to try to interpolate Output values in standard Scala string interpolators.")),(0,i.yg)("admonition",{type:"info"},(0,i.yg)("p",{parentName:"admonition"},"To use the compiler plugin in ",(0,i.yg)("inlineCode",{parentName:"p"},"scala-cli"),", add the following directive to your build configuration file:"),(0,i.yg)("pre",{parentName:"admonition"},(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'//> using plugin "org.virtuslab::besom-compiler-plugin:0.4.0-SNAPSHOT"\n')),(0,i.yg)("p",{parentName:"admonition"},"To use the compiler plugin in ",(0,i.yg)("inlineCode",{parentName:"p"},"sbt"),", add the following line to your ",(0,i.yg)("inlineCode",{parentName:"p"},"build.sbt")," file:"),(0,i.yg)("pre",{parentName:"admonition"},(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'addCompilerPlugin("org.virtuslab" %% "besom-compiler-plugin" % "0.4.0-SNAPSHOT")\n'))))}m.isMDXComponent=!0}}]);