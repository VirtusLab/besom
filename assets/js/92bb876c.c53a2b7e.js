"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[184],{5680:(e,t,a)=>{a.d(t,{xA:()=>u,yg:()=>d});var n=a(6540);function r(e,t,a){return t in e?Object.defineProperty(e,t,{value:a,enumerable:!0,configurable:!0,writable:!0}):e[t]=a,e}function o(e,t){var a=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),a.push.apply(a,n)}return a}function l(e){for(var t=1;t<arguments.length;t++){var a=null!=arguments[t]?arguments[t]:{};t%2?o(Object(a),!0).forEach((function(t){r(e,t,a[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(a)):o(Object(a)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(a,t))}))}return e}function i(e,t){if(null==e)return{};var a,n,r=function(e,t){if(null==e)return{};var a,n,r={},o=Object.keys(e);for(n=0;n<o.length;n++)a=o[n],t.indexOf(a)>=0||(r[a]=e[a]);return r}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)a=o[n],t.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(e,a)&&(r[a]=e[a])}return r}var p=n.createContext({}),s=function(e){var t=n.useContext(p),a=t;return e&&(a="function"==typeof e?e(t):l(l({},t),e)),a},u=function(e){var t=s(e.components);return n.createElement(p.Provider,{value:t},e.children)},m="mdxType",c={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},g=n.forwardRef((function(e,t){var a=e.components,r=e.mdxType,o=e.originalType,p=e.parentName,u=i(e,["components","mdxType","originalType","parentName"]),m=s(a),g=r,d=m["".concat(p,".").concat(g)]||m[g]||c[g]||o;return a?n.createElement(d,l(l({ref:t},u),{},{components:a})):n.createElement(d,l({ref:t},u))}));function d(e,t){var a=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var o=a.length,l=new Array(o);l[0]=g;var i={};for(var p in t)hasOwnProperty.call(t,p)&&(i[p]=t[p]);i.originalType=e,i[m]="string"==typeof e?e:r,l[1]=i;for(var s=2;s<o;s++)l[s]=a[s];return n.createElement.apply(null,l)}return n.createElement.apply(null,a)}g.displayName="MDXCreateElement"},6543:(e,t,a)=>{a.d(t,{A:()=>o});var n=a(6540),r=a(4586);const o=()=>{const{siteConfig:e}=(0,r.A)(),{besomVersion:t}=e.customFields;return n.createElement(n.Fragment,null,t)}},7528:(e,t,a)=>{a.r(t),a.d(t,{assets:()=>p,contentTitle:()=>l,default:()=>c,frontMatter:()=>o,metadata:()=>i,toc:()=>s});var n=a(8168),r=(a(6540),a(5680));a(6543);const o={title:"Getting started"},l=void 0,i={unversionedId:"getting_started",id:"getting_started",title:"Getting started",description:"To start your adventure with infrastructure-as-code with Scala follow these steps:",source:"@site/docs/getting_started.md",sourceDirName:".",slug:"/getting_started",permalink:"/besom/docs/getting_started",draft:!1,editUrl:"https://github.com/VirtusLab/besom/tree/main/website/docs/getting_started.md",tags:[],version:"current",frontMatter:{title:"Getting started"},sidebar:"docsSidebar",previous:{title:"Introduction",permalink:"/besom/docs/intro"},next:{title:"Tutorial",permalink:"/besom/docs/tutorial"}},p={},s=[{value:"Choice of build tool and IDE",id:"choice-of-build-tool-and-ide",level:2}],u={toc:s},m="wrapper";function c(e){let{components:t,...a}=e;return(0,r.yg)(m,(0,n.A)({},u,a,{components:t,mdxType:"MDXLayout"}),(0,r.yg)("p",null,"To start your adventure with infrastructure-as-code with Scala follow these steps:"),(0,r.yg)("ol",null,(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Install Pulumi CLI"),":"),(0,r.yg)("p",{parentName:"li"},"To install the latest Pulumi release, run the following (see full\n",(0,r.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/reference/install/"},"installation instructions")," for additional installation options):"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"curl -fsSL https://get.pulumi.com/ | sh\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Install Scala CLI"),":"),(0,r.yg)("p",{parentName:"li"},"To install the latest Scala CLI release, run the following (see\n",(0,r.yg)("a",{parentName:"p",href:"https://scala-cli.virtuslab.org/install"},"installation instructions")," for additional installation options):"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"curl -sSLf https://scala-cli.virtuslab.org/get | sh\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Install Scala Language Plugin in Pulumi"),":"),(0,r.yg)("p",{parentName:"li"},"To install the latest Scala Language Plugin release, run the following:"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi plugin install language scala 0.3.2 --server github://api.github.com/VirtusLab/besom\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Create a new project"),":"),(0,r.yg)("p",{parentName:"li"},"You can start writing your Besom code at this point, but to help you set up\nBesom comes with ",(0,r.yg)("a",{parentName:"p",href:"/besom/docs/templates"},"Pulumi templates"),"."),(0,r.yg)("p",{parentName:"li"},"You can get started with the ",(0,r.yg)("inlineCode",{parentName:"p"},"pulumi new")," command:"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"mkdir besom-demo && cd besom-demo\n")),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi new https://github.com/VirtusLab/besom/tree/v0.3.2/templates/aws\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Deploy to the Cloud"),":"),(0,r.yg)("p",{parentName:"li"},"Run ",(0,r.yg)("inlineCode",{parentName:"p"},"pulumi up")," to get your code to the cloud:"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi up\n")),(0,r.yg)("p",{parentName:"li"},"This makes all cloud resources declared in your code. Simply make\nedits to your project, and subsequent ",(0,r.yg)("inlineCode",{parentName:"p"},"pulumi up"),"s will compute\nthe minimal diff to deploy your changes.")),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Use Your Program"),":"),(0,r.yg)("p",{parentName:"li"},"Now that your code is deployed, you can interact with it. In the\nabove example, we can find the name of the newly provisioned S3\nbucket:"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi stack output bucketName\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Destroy your Resources"),":"),(0,r.yg)("p",{parentName:"li"},"After you're done, you can remove all resources created by your program:"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi destroy -y\n")))),(0,r.yg)("h2",{id:"choice-of-build-tool-and-ide"},"Choice of build tool and IDE"),(0,r.yg)("hr",null),(0,r.yg)("p",null,"Besom uses ",(0,r.yg)("a",{parentName:"p",href:"https://scala-cli.virtuslab.org/"},"Scala-CLI")," for project compilation and execution."),(0,r.yg)("p",null,"To set up IDE support for an infrastructural project using Besom execute this command\ninside the directory in which Besom project files exist:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"scala-cli setup-ide .\n")),(0,r.yg)("p",null,"As a result of this command, a ",(0,r.yg)("inlineCode",{parentName:"p"},".bsp")," directory will be created inside the project directory."),(0,r.yg)("p",null,"When opened, both ",(0,r.yg)("a",{parentName:"p",href:"https://www.jetbrains.com/idea/"},"Intellij IDEA"),"\nand ",(0,r.yg)("a",{parentName:"p",href:"https://scalameta.org/metals/"},"Metals")," should automatically recognize\nthe project and set up the IDE accordingly."),(0,r.yg)("p",null,(0,r.yg)("a",{parentName:"p",href:"https://www.scala-sbt.org/"},"sbt"),", ",(0,r.yg)("a",{parentName:"p",href:"https://gradle.org/"},"gradle")," and ",(0,r.yg)("a",{parentName:"p",href:"https://maven.apache.org/"},"maven")," are also supported out-of-the-box,\nbut are ",(0,r.yg)("strong",{parentName:"p"},"not recommended")," due to slower iteration speed.\nUse of ",(0,r.yg)("inlineCode",{parentName:"p"},"sbt"),", ",(0,r.yg)("inlineCode",{parentName:"p"},"gradle")," or ",(0,r.yg)("inlineCode",{parentName:"p"},"mvn")," support is suggested for situations where managed infrastructure\nis being added to an already existing project that uses sbt as the main build tool."),(0,r.yg)("p",null,"IDE setup for ",(0,r.yg)("inlineCode",{parentName:"p"},"sbt"),", ",(0,r.yg)("inlineCode",{parentName:"p"},"gradle")," or ",(0,r.yg)("inlineCode",{parentName:"p"},"mvn")," works automatically with both Intellij IDEA and Metals."),(0,r.yg)("p",null,(0,r.yg)("a",{parentName:"p",href:"https://mill-build.com/"},"Mill")," is not yet supported."))}c.isMDXComponent=!0}}]);