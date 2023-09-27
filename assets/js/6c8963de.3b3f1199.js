"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[156],{3905:(e,t,n)=>{n.d(t,{Zo:()=>c,kt:()=>d});var r=n(7294);function a(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function o(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?o(Object(n),!0).forEach((function(t){a(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):o(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function s(e,t){if(null==e)return{};var n,r,a=function(e,t){if(null==e)return{};var n,r,a={},o=Object.keys(e);for(r=0;r<o.length;r++)n=o[r],t.indexOf(n)>=0||(a[n]=e[n]);return a}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(r=0;r<o.length;r++)n=o[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(a[n]=e[n])}return a}var l=r.createContext({}),p=function(e){var t=r.useContext(l),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},c=function(e){var t=p(e.components);return r.createElement(l.Provider,{value:t},e.children)},u="mdxType",f={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},m=r.forwardRef((function(e,t){var n=e.components,a=e.mdxType,o=e.originalType,l=e.parentName,c=s(e,["components","mdxType","originalType","parentName"]),u=p(n),m=a,d=u["".concat(l,".").concat(m)]||u[m]||f[m]||o;return n?r.createElement(d,i(i({ref:t},c),{},{components:n})):r.createElement(d,i({ref:t},c))}));function d(e,t){var n=arguments,a=t&&t.mdxType;if("string"==typeof e||a){var o=n.length,i=new Array(o);i[0]=m;var s={};for(var l in t)hasOwnProperty.call(t,l)&&(s[l]=t[l]);s.originalType=e,s[u]="string"==typeof e?e:a,i[1]=s;for(var p=2;p<o;p++)i[p]=n[p];return r.createElement.apply(null,i)}return r.createElement.apply(null,n)}m.displayName="MDXCreateElement"},6953:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>l,contentTitle:()=>i,default:()=>f,frontMatter:()=>o,metadata:()=>s,toc:()=>p});var r=n(7462),a=(n(7294),n(3905));const o={title:"Lifting"},i=void 0,s={unversionedId:"lifting",id:"lifting",title:"Lifting",description:"Pulumi supports a feature called lifting which allows the user to access properties of data structures held by an Output as if the value was not inside of the asynchronous datatype. Let's look at our handy S3 bucket again:",source:"@site/docs/lifting.md",sourceDirName:".",slug:"/lifting",permalink:"/besom/docs/lifting",draft:!1,tags:[],version:"current",frontMatter:{title:"Lifting"},sidebar:"docsSidebar",previous:{title:"Logging",permalink:"/besom/docs/logging"},next:{title:"String interpolation",permalink:"/besom/docs/interpolator"}},l={},p=[],c={toc:p},u="wrapper";function f(e){let{components:t,...n}=e;return(0,a.kt)(u,(0,r.Z)({},c,n,{components:t,mdxType:"MDXLayout"}),(0,a.kt)("p",null,"Pulumi supports a feature called ",(0,a.kt)("em",{parentName:"p"},"lifting")," which allows the user to access properties of data structures held by an Output as if the value was not inside of the asynchronous datatype. Let's look at our handy S3 bucket again:"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-scala"},'import besom.*\nimport besom.api.aws\n\u200b\n@main def main = Pulumi.run {\n  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.Bucket("my-bucket")\n\u200b\n  Output(Pulumi.exports(s3Url = s3Bucket.map(_.websiteEndpoint)))\n}\n')),(0,a.kt)("p",null,"As you can see here we're accessing the property ",(0,a.kt)("inlineCode",{parentName:"p"},"websiteEndpoint")," on ",(0,a.kt)("inlineCode",{parentName:"p"},"aws.s3.Bucket")," class by first ",(0,a.kt)("inlineCode",{parentName:"p"},"map"),"ping over the Output. This syntax can be replaced in Besom thanks to first class support of ",(0,a.kt)("em",{parentName:"p"},"lifting")," (via Scala 3 extension methods generated in packages for Besom):"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-scala"},"extension (o: Output[aws.s3.Bucket])\n  def url: Output[String] = o.map(_.websiteEndpoint) \n")),(0,a.kt)("p",null,"This allows for this syntax:"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-scala"},"Output(Pulumi.exports(s3Url = s3Bucket.websiteEndpoint))\n")),(0,a.kt)("p",null,"These lifted syntaxes cover more cases and work recursively so you can access even the properties on nested data structures like ",(0,a.kt)("inlineCode",{parentName:"p"},"a.b.c.d")," with a direct syntax."))}f.isMDXComponent=!0}}]);