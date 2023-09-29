"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[819],{3905:(e,t,r)=>{r.d(t,{Zo:()=>p,kt:()=>d});var n=r(7294);function o(e,t,r){return t in e?Object.defineProperty(e,t,{value:r,enumerable:!0,configurable:!0,writable:!0}):e[t]=r,e}function a(e,t){var r=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),r.push.apply(r,n)}return r}function s(e){for(var t=1;t<arguments.length;t++){var r=null!=arguments[t]?arguments[t]:{};t%2?a(Object(r),!0).forEach((function(t){o(e,t,r[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(r)):a(Object(r)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(r,t))}))}return e}function c(e,t){if(null==e)return{};var r,n,o=function(e,t){if(null==e)return{};var r,n,o={},a=Object.keys(e);for(n=0;n<a.length;n++)r=a[n],t.indexOf(r)>=0||(o[r]=e[r]);return o}(e,t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(n=0;n<a.length;n++)r=a[n],t.indexOf(r)>=0||Object.prototype.propertyIsEnumerable.call(e,r)&&(o[r]=e[r])}return o}var i=n.createContext({}),u=function(e){var t=n.useContext(i),r=t;return e&&(r="function"==typeof e?e(t):s(s({},t),e)),r},p=function(e){var t=u(e.components);return n.createElement(i.Provider,{value:t},e.children)},l="mdxType",m={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},f=n.forwardRef((function(e,t){var r=e.components,o=e.mdxType,a=e.originalType,i=e.parentName,p=c(e,["components","mdxType","originalType","parentName"]),l=u(r),f=o,d=l["".concat(i,".").concat(f)]||l[f]||m[f]||a;return r?n.createElement(d,s(s({ref:t},p),{},{components:r})):n.createElement(d,s({ref:t},p))}));function d(e,t){var r=arguments,o=t&&t.mdxType;if("string"==typeof e||o){var a=r.length,s=new Array(a);s[0]=f;var c={};for(var i in t)hasOwnProperty.call(t,i)&&(c[i]=t[i]);c.originalType=e,c[l]="string"==typeof e?e:o,s[1]=c;for(var u=2;u<a;u++)s[u]=r[u];return n.createElement.apply(null,s)}return n.createElement.apply(null,r)}f.displayName="MDXCreateElement"},2349:(e,t,r)=>{r.r(t),r.d(t,{assets:()=>i,contentTitle:()=>s,default:()=>m,frontMatter:()=>a,metadata:()=>c,toc:()=>u});var n=r(7462),o=(r(7294),r(3905));const a={title:"Exports"},s=void 0,c={unversionedId:"exports",id:"exports",title:"Exports",description:"Exports a.k.a. Stack Outputs",source:"@site/docs/exports.md",sourceDirName:".",slug:"/exports",permalink:"/besom/docs/exports",draft:!1,tags:[],version:"current",frontMatter:{title:"Exports"},sidebar:"docsSidebar",previous:{title:"Context and imports",permalink:"/besom/docs/context"},next:{title:"Resource constructors, outputs and asynchronicity",permalink:"/besom/docs/constructors"}},i={},u=[],p={toc:u},l="wrapper";function m(e){let{components:t,...r}=e;return(0,o.kt)(l,(0,n.Z)({},p,r,{components:t,mdxType:"MDXLayout"}),(0,o.kt)("p",null,"Exports a.k.a. ",(0,o.kt)("a",{parentName:"p",href:"https://www.pulumi.com/learn/building-with-pulumi/stack-outputs/"},"Stack Outputs"),"\nare a way to share values between stacks."),(0,o.kt)("p",null,"In other SDKs you are free to call an ",(0,o.kt)("inlineCode",{parentName:"p"},"export")," method on the Pulumi Context object whenever you want in a program.\nBesom's functional design disallows this - since your program is a function exported keys and values have to be\nthe last value your main function returns, e.g.:"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala"},'import besom.*\nimport besom.api.aws\n\n@main def run = Pulumi.run {\n  for\n    bucket <- aws.s3.Bucket("my-bucket", ...) \n  yield Pulumi.exports(\n    bucketUrl = bucket.websiteEndpoint\n  )\n}\n')))}m.isMDXComponent=!0}}]);