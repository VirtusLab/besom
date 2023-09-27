"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[347],{3905:(e,t,a)=>{a.d(t,{Zo:()=>u,kt:()=>g});var n=a(7294);function r(e,t,a){return t in e?Object.defineProperty(e,t,{value:a,enumerable:!0,configurable:!0,writable:!0}):e[t]=a,e}function o(e,t){var a=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),a.push.apply(a,n)}return a}function l(e){for(var t=1;t<arguments.length;t++){var a=null!=arguments[t]?arguments[t]:{};t%2?o(Object(a),!0).forEach((function(t){r(e,t,a[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(a)):o(Object(a)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(a,t))}))}return e}function i(e,t){if(null==e)return{};var a,n,r=function(e,t){if(null==e)return{};var a,n,r={},o=Object.keys(e);for(n=0;n<o.length;n++)a=o[n],t.indexOf(a)>=0||(r[a]=e[a]);return r}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)a=o[n],t.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(e,a)&&(r[a]=e[a])}return r}var p=n.createContext({}),s=function(e){var t=n.useContext(p),a=t;return e&&(a="function"==typeof e?e(t):l(l({},t),e)),a},u=function(e){var t=s(e.components);return n.createElement(p.Provider,{value:t},e.children)},c="mdxType",m={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},d=n.forwardRef((function(e,t){var a=e.components,r=e.mdxType,o=e.originalType,p=e.parentName,u=i(e,["components","mdxType","originalType","parentName"]),c=s(a),d=r,g=c["".concat(p,".").concat(d)]||c[d]||m[d]||o;return a?n.createElement(g,l(l({ref:t},u),{},{components:a})):n.createElement(g,l({ref:t},u))}));function g(e,t){var a=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var o=a.length,l=new Array(o);l[0]=d;var i={};for(var p in t)hasOwnProperty.call(t,p)&&(i[p]=t[p]);i.originalType=e,i[c]="string"==typeof e?e:r,l[1]=i;for(var s=2;s<o;s++)l[s]=a[s];return n.createElement.apply(null,l)}return n.createElement.apply(null,a)}d.displayName="MDXCreateElement"},9858:(e,t,a)=>{a.r(t),a.d(t,{assets:()=>p,contentTitle:()=>l,default:()=>m,frontMatter:()=>o,metadata:()=>i,toc:()=>s});var n=a(7462),r=(a(7294),a(3905));a(2263);const o={title:"Getting started"},l=void 0,i={unversionedId:"getting_started",id:"getting_started",title:"Getting started",description:"To start your adventure with infrastructure-as-code with Scala follow these steps:",source:"@site/docs/getting_started.md",sourceDirName:".",slug:"/getting_started",permalink:"/besom/docs/getting_started",draft:!1,tags:[],version:"current",frontMatter:{title:"Getting started"},sidebar:"docsSidebar",previous:{title:"Introduction",permalink:"/besom/docs/intro"},next:{title:"Tutorial",permalink:"/besom/docs/tutorial"}},p={},s=[],u={toc:s},c="wrapper";function m(e){let{components:t,...a}=e;return(0,r.kt)(c,(0,n.Z)({},u,a,{components:t,mdxType:"MDXLayout"}),(0,r.kt)("p",null,"To start your adventure with infrastructure-as-code with Scala follow these steps:"),(0,r.kt)("ol",null,(0,r.kt)("li",{parentName:"ol"},(0,r.kt)("p",{parentName:"li"},(0,r.kt)("strong",{parentName:"p"},"Install Pulumi CLI"),":"),(0,r.kt)("p",{parentName:"li"},"To install the latest Pulumi release, run the following (see full\n",(0,r.kt)("a",{parentName:"p",href:"https://www.pulumi.com/docs/reference/install/"},"installation instructions")," for additional installation options):"),(0,r.kt)("pre",{parentName:"li"},(0,r.kt)("code",{parentName:"pre",className:"language-bash"},"curl -fsSL https://get.pulumi.com/ | sh\n"))),(0,r.kt)("li",{parentName:"ol"},(0,r.kt)("p",{parentName:"li"},(0,r.kt)("strong",{parentName:"p"},"Install Scala CLI"),":"),(0,r.kt)("p",{parentName:"li"},"To install the latest Scala CLI release, see\n",(0,r.kt)("a",{parentName:"p",href:"https://scala-cli.virtuslab.org/install"},"installation instructions")," for installation options.")),(0,r.kt)("li",{parentName:"ol"},(0,r.kt)("p",{parentName:"li"},(0,r.kt)("strong",{parentName:"p"},"Install Scala Language Plugin in Pulumi"),":"),(0,r.kt)("p",{parentName:"li"},"To install the latest Scala Language Plugin release, run the following:"),(0,r.kt)("pre",{parentName:"li"},(0,r.kt)("code",{parentName:"pre"},"pulumi plugin install language scala 0.1.0 --server github://api.github.com/VirtusLab/besom \n"))),(0,r.kt)("li",{parentName:"ol"},(0,r.kt)("p",{parentName:"li"},(0,r.kt)("strong",{parentName:"p"},"Create a new project"),":\nYou can start writing your Besom code at this point, but to help you set up\nBesom comes with ",(0,r.kt)("a",{parentName:"p",href:"./templates"},"Pulumi templates"),"."),(0,r.kt)("p",{parentName:"li"},"You can get started with the ",(0,r.kt)("inlineCode",{parentName:"p"},"pulumi new")," command:"),(0,r.kt)("pre",{parentName:"li"},(0,r.kt)("code",{parentName:"pre",className:"language-bash"},"mkdir besom-demo && cd besom-demo\n")),(0,r.kt)("pre",{parentName:"li"},(0,r.kt)("code",{parentName:"pre",className:"language-bash"},"pulumi new https://github.com/VirtusLab/besom/tree/develop/template/default\n"))),(0,r.kt)("li",{parentName:"ol"},(0,r.kt)("p",{parentName:"li"},(0,r.kt)("strong",{parentName:"p"},"Deploy to the Cloud"),":\nRun ",(0,r.kt)("inlineCode",{parentName:"p"},"pulumi up")," to get your code to the cloud:"),(0,r.kt)("pre",{parentName:"li"},(0,r.kt)("code",{parentName:"pre",className:"language-bash"},"pulumi up\n")),(0,r.kt)("p",{parentName:"li"},"This makes all cloud resources declared in your code. Simply make\nedits to your project, and subsequent ",(0,r.kt)("inlineCode",{parentName:"p"},"pulumi up"),"s will compute\nthe minimal diff to deploy your changes.")),(0,r.kt)("li",{parentName:"ol"},(0,r.kt)("p",{parentName:"li"},(0,r.kt)("strong",{parentName:"p"},"Use Your Program"),":\nNow that your code is deployed, you can interact with it. In the\nabove example, we can find the name of the newly provisioned S3\nbucket:"),(0,r.kt)("pre",{parentName:"li"},(0,r.kt)("code",{parentName:"pre",className:"language-bash"},"$ pulumi stack output bucketName\n")))))}m.isMDXComponent=!0}}]);