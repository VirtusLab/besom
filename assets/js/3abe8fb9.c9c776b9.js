"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[485],{5680:(e,t,n)=>{n.d(t,{xA:()=>c,yg:()=>y});var r=n(6540);function o(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function i(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function a(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?i(Object(n),!0).forEach((function(t){o(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):i(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function l(e,t){if(null==e)return{};var n,r,o=function(e,t){if(null==e)return{};var n,r,o={},i=Object.keys(e);for(r=0;r<i.length;r++)n=i[r],t.indexOf(n)>=0||(o[n]=e[n]);return o}(e,t);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(r=0;r<i.length;r++)n=i[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(o[n]=e[n])}return o}var s=r.createContext({}),p=function(e){var t=r.useContext(s),n=t;return e&&(n="function"==typeof e?e(t):a(a({},t),e)),n},c=function(e){var t=p(e.components);return r.createElement(s.Provider,{value:t},e.children)},u="mdxType",g={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},m=r.forwardRef((function(e,t){var n=e.components,o=e.mdxType,i=e.originalType,s=e.parentName,c=l(e,["components","mdxType","originalType","parentName"]),u=p(n),m=o,y=u["".concat(s,".").concat(m)]||u[m]||g[m]||i;return n?r.createElement(y,a(a({ref:t},c),{},{components:n})):r.createElement(y,a({ref:t},c))}));function y(e,t){var n=arguments,o=t&&t.mdxType;if("string"==typeof e||o){var i=n.length,a=new Array(i);a[0]=m;var l={};for(var s in t)hasOwnProperty.call(t,s)&&(l[s]=t[s]);l.originalType=e,l[u]="string"==typeof e?e:o,a[1]=l;for(var p=2;p<i;p++)a[p]=n[p];return r.createElement.apply(null,a)}return r.createElement.apply(null,n)}m.displayName="MDXCreateElement"},6083:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>s,contentTitle:()=>a,default:()=>g,frontMatter:()=>i,metadata:()=>l,toc:()=>p});var r=n(8168),o=(n(6540),n(5680));const i={title:"Logging"},a=void 0,l={unversionedId:"logging",id:"logging",title:"Logging",description:"In every scope where Pulumi Context is available and global Besom import was included user has the capability to summon",source:"@site/docs/logging.md",sourceDirName:".",slug:"/logging",permalink:"/besom/docs/logging",draft:!1,tags:[],version:"current",frontMatter:{title:"Logging"},sidebar:"docsSidebar",previous:{title:"Apply methods",permalink:"/besom/docs/apply_methods"},next:{title:"Lifting",permalink:"/besom/docs/lifting"}},s={},p=[{value:"Why not simply <code>println</code>?",id:"why-not-simply-println",level:2}],c={toc:p},u="wrapper";function g(e){let{components:t,...n}=e;return(0,o.yg)(u,(0,r.A)({},c,n,{components:t,mdxType:"MDXLayout"}),(0,o.yg)("p",null,"In every scope where Pulumi ",(0,o.yg)("inlineCode",{parentName:"p"},"Context")," is available and global Besom import was included user has the capability to summon\nlogger by writing ",(0,o.yg)("inlineCode",{parentName:"p"},"log")," with a following severity level used as a logging method's name, e.g.: "),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'@main def run = Pulumi.run {\n  Stack(log.warn("Nothing to do."))\n}\n')),(0,o.yg)("p",null,"Logging is an asynchronous, effectful operation and therefore returns an ",(0,o.yg)("inlineCode",{parentName:"p"},"Output"),". This means that all logging statements need to be composed\ninto other values that will eventually be either passed as ",(0,o.yg)("inlineCode",{parentName:"p"},"Stack")," arguments or exports. This is similar to how logging frameworks for ",(0,o.yg)("inlineCode",{parentName:"p"},"cats")," or ",(0,o.yg)("inlineCode",{parentName:"p"},"ZIO")," behave (eg.: ",(0,o.yg)("a",{parentName:"p",href:"https://github.com/typelevel/log4cats"},"log4cats"),")."),(0,o.yg)("h2",{id:"why-not-simply-println"},"Why not simply ",(0,o.yg)("inlineCode",{parentName:"h2"},"println"),"?"),(0,o.yg)("p",null,"Given that you're working with CLI you might be tempted to just ",(0,o.yg)("inlineCode",{parentName:"p"},"println")," some value, but that will have no visible effect.\nThat's because Besom's Scala code is being executed in a different process than Pulumi. It's Pulumi that drives the\nprocess by calling Besom. Therefore, you have to use functions provided by Besom for your code to log anything."))}g.isMDXComponent=!0}}]);