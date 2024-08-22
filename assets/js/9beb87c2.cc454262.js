"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[539],{5680:(e,t,a)=>{a.d(t,{xA:()=>u,yg:()=>d});var n=a(6540);function i(e,t,a){return t in e?Object.defineProperty(e,t,{value:a,enumerable:!0,configurable:!0,writable:!0}):e[t]=a,e}function r(e,t){var a=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),a.push.apply(a,n)}return a}function o(e){for(var t=1;t<arguments.length;t++){var a=null!=arguments[t]?arguments[t]:{};t%2?r(Object(a),!0).forEach((function(t){i(e,t,a[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(a)):r(Object(a)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(a,t))}))}return e}function l(e,t){if(null==e)return{};var a,n,i=function(e,t){if(null==e)return{};var a,n,i={},r=Object.keys(e);for(n=0;n<r.length;n++)a=r[n],t.indexOf(a)>=0||(i[a]=e[a]);return i}(e,t);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);for(n=0;n<r.length;n++)a=r[n],t.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(e,a)&&(i[a]=e[a])}return i}var s=n.createContext({}),p=function(e){var t=n.useContext(s),a=t;return e&&(a="function"==typeof e?e(t):o(o({},t),e)),a},u=function(e){var t=p(e.components);return n.createElement(s.Provider,{value:t},e.children)},m="mdxType",g={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},c=n.forwardRef((function(e,t){var a=e.components,i=e.mdxType,r=e.originalType,s=e.parentName,u=l(e,["components","mdxType","originalType","parentName"]),m=p(a),c=i,d=m["".concat(s,".").concat(c)]||m[c]||g[c]||r;return a?n.createElement(d,o(o({ref:t},u),{},{components:a})):n.createElement(d,o({ref:t},u))}));function d(e,t){var a=arguments,i=t&&t.mdxType;if("string"==typeof e||i){var r=a.length,o=new Array(r);o[0]=c;var l={};for(var s in t)hasOwnProperty.call(t,s)&&(l[s]=t[s]);l.originalType=e,l[m]="string"==typeof e?e:i,o[1]=l;for(var p=2;p<r;p++)o[p]=a[p];return n.createElement.apply(null,o)}return n.createElement.apply(null,a)}c.displayName="MDXCreateElement"},1636:(e,t,a)=>{a.r(t),a.d(t,{assets:()=>s,contentTitle:()=>o,default:()=>g,frontMatter:()=>r,metadata:()=>l,toc:()=>p});var n=a(8168),i=(a(6540),a(5680));const r={title:"Changelog"},o=void 0,l={unversionedId:"changelog",id:"changelog",title:"Changelog",description:"0.3.2 (11-06-2024)",source:"@site/docs/changelog.md",sourceDirName:".",slug:"/changelog",permalink:"/besom/docs/changelog",draft:!1,editUrl:"https://github.com/VirtusLab/besom/tree/main/website/docs/changelog.md",tags:[],version:"current",frontMatter:{title:"Changelog"},sidebar:"docsSidebar",previous:{title:"Templates",permalink:"/besom/docs/templates"}},s={},p=[{value:"0.3.2 (11-06-2024)",id:"032-11-06-2024",level:2},{value:"0.3.1 (19-04-2024)",id:"031-19-04-2024",level:2},{value:"0.3.0 (16-04-2024)",id:"030-16-04-2024",level:2},{value:"API Changes and New Features",id:"api-changes-and-new-features",level:3},{value:"Bug Fixes",id:"bug-fixes",level:3},{value:"Other Changes",id:"other-changes",level:3},{value:"0.2.2 (22-02-2024)",id:"022-22-02-2024",level:2},{value:"Bug Fixes",id:"bug-fixes-1",level:3},{value:"Other Changes",id:"other-changes-1",level:3},{value:"0.2.1 (15-02-2024)",id:"021-15-02-2024",level:2},{value:"Bug Fixes",id:"bug-fixes-2",level:3},{value:"0.2.0 (08-02-2024)",id:"020-08-02-2024",level:2},{value:"API Changes",id:"api-changes",level:3},{value:"New Features",id:"new-features",level:3},{value:"Bug Fixes",id:"bug-fixes-3",level:3},{value:"Other Changes",id:"other-changes-2",level:3}],u={toc:p},m="wrapper";function g(e){let{components:t,...a}=e;return(0,i.yg)(m,(0,n.A)({},u,a,{components:t,mdxType:"MDXLayout"}),(0,i.yg)("h2",{id:"032-11-06-2024"},"0.3.2 (11-06-2024)"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"Added missing ",(0,i.yg)("inlineCode",{parentName:"li"},"provider")," / ",(0,i.yg)("inlineCode",{parentName:"li"},"providers")," field on resources to fix #397 (@lbialy in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/505"},"505"),")"),(0,i.yg)("li",{parentName:"ul"},"Exposed ",(0,i.yg)("inlineCode",{parentName:"li"},"CustomTimeouts")," resource option configuration value (@lbialy in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/502"},"502"),")"),(0,i.yg)("li",{parentName:"ul"},"Fixed issue 489 where ANSI control chars crashed internal logging (@lbialy in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/492"},"492"),")"),(0,i.yg)("li",{parentName:"ul"},"First working draft of Automation API (sync, Either-monad based) (@pawelprazak in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/336"},"336"),")"),(0,i.yg)("li",{parentName:"ul"},"Fixed issues with eviction of dependencies of core besom library (@lbialy in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/500"},"500"),")"),(0,i.yg)("li",{parentName:"ul"},"Fixed bug that crashed besom when resource options were provided for component resource (@lbialy in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/502"},"502"),")"),(0,i.yg)("li",{parentName:"ul"},"First working draft of Besom Configure (besom-cfg) (Kubernetes only) (@lbialy in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/494"},"494"),")"),(0,i.yg)("li",{parentName:"ul"},"Allow passing options to scala-cli via language plugin executor (@lbialy in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/503"},"503"),")")),(0,i.yg)("p",null,"New examples:"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"Add AWS API Gateway V2 with EventBridge example by @polkx in ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/479"},"https://github.com/VirtusLab/besom/pull/479"))),(0,i.yg)("p",null,(0,i.yg)("strong",{parentName:"p"},"Full Changelog"),": ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/compare/v0.3.1...v0.3.2"},"https://github.com/VirtusLab/besom/compare/v0.3.1...v0.3.2")),(0,i.yg)("h2",{id:"031-19-04-2024"},"0.3.1 (19-04-2024)"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Support for ",(0,i.yg)("inlineCode",{parentName:"p"},"ConfigGroup"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"ConfigFile")," resources in ",(0,i.yg)("a",{parentName:"p",href:"https://www.pulumi.com/blog/kubernetes-yaml-v2/"},"Kubernetes provider 4.11.0+"),"\nis now available in Besom!")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Added new combinators on ",(0,i.yg)("inlineCode",{parentName:"p"},"Output"),":"),(0,i.yg)("ul",{parentName:"li"},(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},"recover")," allows to map an error inside of a failed ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")," to a new value"),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},"recoverWith")," allows the same but using an effectful function returning either an ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")," or any\nother supported effect, e.g.: ",(0,i.yg)("inlineCode",{parentName:"li"},"Future"),", ",(0,i.yg)("inlineCode",{parentName:"li"},"IO")," or ",(0,i.yg)("inlineCode",{parentName:"li"},"Task")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},"tap")," allows to access the value of an ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")," and apply an effectful function to it while\ndiscarding said function's results"),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},"tapError")," allows the same but for an error of a failed ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},"tapBoth")," takes two effectful function and allows to access either error or value of an ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")," by\napplying one of them to the contents of the ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},"void")," discards the value of an ",(0,i.yg)("inlineCode",{parentName:"li"},"Output"),", comes with a static constructor function - ",(0,i.yg)("inlineCode",{parentName:"li"},"Output.unit")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},"unzip")," can be called on an ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")," of a tuple to receive a tuple of ",(0,i.yg)("inlineCode",{parentName:"li"},"Output"),"s, all of which are\ndescendents of the original ",(0,i.yg)("inlineCode",{parentName:"li"},"Output"))))),(0,i.yg)("p",null,(0,i.yg)("strong",{parentName:"p"},"Full Changelog"),": ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/compare/v0.3.0...v0.3.1"},"GitHub (v0.3.0...v0.3.1)")),(0,i.yg)("h2",{id:"030-16-04-2024"},"0.3.0 (16-04-2024)"),(0,i.yg)("h3",{id:"api-changes-and-new-features"},"API Changes and New Features"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"Added new ",(0,i.yg)("inlineCode",{parentName:"li"},"besom.json")," interpolation API. Now this snippet from our tutorial:")),(0,i.yg)("pre",null,(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'s3.BucketPolicyArgs(\n  bucket = feedBucket.id,\n  policy = JsObject(\n    "Version" -> JsString("2012-10-17"),\n    "Statement" -> JsArray(\n      JsObject(\n        "Sid" -> JsString("PublicReadGetObject"),\n        "Effect" -> JsString("Allow"),\n        "Principal" -> JsObject(\n          "AWS" -> JsString("*")\n        ),\n        "Action" -> JsArray(JsString("s3:GetObject")),\n        "Resource" -> JsArray(JsString(s"arn:aws:s3:::${name}/*"))\n      )\n    )\n  ).prettyPrint\n)\n')),(0,i.yg)("p",null,"can be rewritten as:"),(0,i.yg)("pre",null,(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'s3.BucketPolicyArgs(\n  bucket = feedBucket.id,\n  policy = json"""{\n    "Version": "2012-10-17",\n    "Statement": [{\n      "Sid": "PublicReadGetObject",\n      "Effect": "Allow",\n      "Principal": {\n        "AWS": "*"\n      },\n      "Action": ["s3:GetObject"],\n      "Resource": ["arn:aws:s3::${name}/*"]\n    }]\n  }""".map(_.prettyPrint)\n)\n')),(0,i.yg)("p",null,"The json interpolator returns an ",(0,i.yg)("inlineCode",{parentName:"p"},"Output[JsValue]")," and is fully ",(0,i.yg)("strong",{parentName:"p"},"compile-time type safe")," and verifies JSON string for correctness.\nThe only types that can be interpolated are ",(0,i.yg)("inlineCode",{parentName:"p"},"String"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"Int"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"Short"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"Long"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"Float"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"Double"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"JsValue")," and ",(0,i.yg)("inlineCode",{parentName:"p"},"Option")," and ",(0,i.yg)("inlineCode",{parentName:"p"},"Output"),"\nof the former (in whatever amount of nesting). If you need to interpolate a more complex type it's advised to derive a ",(0,i.yg)("inlineCode",{parentName:"p"},"JsonFormat"),"\nfor it and convert it to ",(0,i.yg)("inlineCode",{parentName:"p"},"JsValue"),"."),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Package ",(0,i.yg)("inlineCode",{parentName:"p"},"besom.json")," was modified to ease the use of ",(0,i.yg)("inlineCode",{parentName:"p"},"JsonFormat")," derivation. This change is breaking compatibility by exporting\ndefault instances from ",(0,i.yg)("inlineCode",{parentName:"p"},"DefaultJsonProtocol")," and providing a given ",(0,i.yg)("inlineCode",{parentName:"p"},"JsonProtocol")," instance for use with ",(0,i.yg)("inlineCode",{parentName:"p"},"derives JsonFormat"),".\nIf you need to define a custom ",(0,i.yg)("inlineCode",{parentName:"p"},"JsonProcol")," change the import to ",(0,i.yg)("inlineCode",{parentName:"p"},"import besom.json.custom.*")," which preserves the older semantics\nfrom spray-json and requires manual extension of ",(0,i.yg)("inlineCode",{parentName:"p"},"DefaultJsonProtocol"),".")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Derived ",(0,i.yg)("inlineCode",{parentName:"p"},"JsonFormat")," from package ",(0,i.yg)("inlineCode",{parentName:"p"},"besom.json")," now respects arguments with default values. In conjunction with the previous change\nit means one can now use it like this:"))),(0,i.yg)("pre",null,(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'import besom.json.*\n\ncase class Color(name: String, red: Int, green: Int, blue: Int = 160) derives JsonFormat\nval color = Color("CadetBlue", 95, 158)\n\nval json = """{"name":"CadetBlue","red":95,"green":158}"""\n\nassert(color.toJson.convertTo[Color] == color)\nassert(json.parseJson.convertTo[Color] == color)\n')),(0,i.yg)("h3",{id:"bug-fixes"},"Bug Fixes"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"fixed infinite loop in encoders ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/407"},"#407")," when a recursive type is encountered "),(0,i.yg)("li",{parentName:"ul"},"fixed cause passing in ",(0,i.yg)("inlineCode",{parentName:"li"},"AggregateException")," to improve debugging of decoders ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/426"},"#426")),(0,i.yg)("li",{parentName:"ul"},"fixed Pulumi side effects memoization issues in Component API ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/429"},"#429")),(0,i.yg)("li",{parentName:"ul"},"fixed traverse problem caused by export bug in compiler with a temporary workaround ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/430"},"#430"))),(0,i.yg)("h3",{id:"other-changes"},"Other Changes"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"custom timeouts have scaladocs now ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/419"},"#419")),(0,i.yg)("li",{parentName:"ul"},"overhauled serde layer with refined outputs implemented to improve parity with upstream Pulumi engine ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/414"},"#414")),(0,i.yg)("li",{parentName:"ul"},"StackReferences are now documented ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/428"},"#428")),(0,i.yg)("li",{parentName:"ul"},"updated AWS EKS hello world example ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/399/files"},"#399")),(0,i.yg)("li",{parentName:"ul"},"Component API now disallows returning component instances wrapped in Outputs to prevent users from dry run issues ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/441"},"#441")),(0,i.yg)("li",{parentName:"ul"},"added ",(0,i.yg)("inlineCode",{parentName:"li"},"parSequence")," and ",(0,i.yg)("inlineCode",{parentName:"li"},"parTraverse")," combinators on ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/440"},"#440")),(0,i.yg)("li",{parentName:"ul"},"added ",(0,i.yg)("inlineCode",{parentName:"li"},"Output.when")," combinator ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/439"},"#439")),(0,i.yg)("li",{parentName:"ul"},"improved compilation errors around ",(0,i.yg)("inlineCode",{parentName:"li"},"Output.eval")," and ",(0,i.yg)("inlineCode",{parentName:"li"},"Output#flatMap")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/443"},"#443")),(0,i.yg)("li",{parentName:"ul"},"all ",(0,i.yg)("inlineCode",{parentName:"li"},"Output")," combinators have scaladocs now ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/445"},"#445")),(0,i.yg)("li",{parentName:"ul"},"added extension-based combinators for ",(0,i.yg)("inlineCode",{parentName:"li"},"Output[Option[A]]"),", ",(0,i.yg)("inlineCode",{parentName:"li"},"Output[List[A]]")," etc ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/445"},"#445")),(0,i.yg)("li",{parentName:"ul"},"added support for overlays (package-specific extensions) in besom ",(0,i.yg)("inlineCode",{parentName:"li"},"codegen"),", this opens a way for support of Helm, magic lambdas and other advanced features ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/402"},"#402"))),(0,i.yg)("p",null,(0,i.yg)("strong",{parentName:"p"},"Full Changelog"),": ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/compare/v0.2.2...v0.3.0"},"GitHub (v0.2.2...v0.3.0)")),(0,i.yg)("h2",{id:"022-22-02-2024"},"0.2.2 (22-02-2024)"),(0,i.yg)("h3",{id:"bug-fixes-1"},"Bug Fixes"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"fixed component argument serialization issue ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/398"},"398"))),(0,i.yg)("h3",{id:"other-changes-1"},"Other Changes"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"added Kubernetes guestbook example ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/395"},"395"))),(0,i.yg)("p",null,(0,i.yg)("strong",{parentName:"p"},"Full Changelog"),": ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/compare/v0.2.1...v0.2.2"},"GitHub (v0.2.1...v0.2.2)")),(0,i.yg)("h2",{id:"021-15-02-2024"},"0.2.1 (15-02-2024)"),(0,i.yg)("h3",{id:"bug-fixes-2"},"Bug Fixes"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"Fix URL validation to allow for kubernetes types ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/385"},"#385")),(0,i.yg)("li",{parentName:"ul"},"Loosen up and fix URN parsing ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/389"},"#389")),(0,i.yg)("li",{parentName:"ul"},"Fix serializer now skips fields with null value secrets ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/386"},"#386"))),(0,i.yg)("p",null,(0,i.yg)("strong",{parentName:"p"},"Full Changelog"),": ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/compare/v0.2.0...v0.2.1"},"GitHub (v0.2.0...v0.2.1)")),(0,i.yg)("h2",{id:"020-08-02-2024"},"0.2.0 (08-02-2024)"),(0,i.yg)("h3",{id:"api-changes"},"API Changes"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Changed the type of main ",(0,i.yg)("inlineCode",{parentName:"p"},"Pulumi.run")," function from ",(0,i.yg)("inlineCode",{parentName:"p"},"Context ?=> Output[Exports]")," (a ",(0,i.yg)("a",{parentName:"p",href:"https://docs.scala-lang.org/scala3/reference/contextual/context-functions.html"},"context function")," providing ",(0,i.yg)("inlineCode",{parentName:"p"},"besom.Context"),"\ninstance implicitly in its scope and expecting ",(0,i.yg)("inlineCode",{parentName:"p"},"Output[Exports]")," as returned value) to ",(0,i.yg)("inlineCode",{parentName:"p"},"Context ?=> Stack"),". This change has one core\nreason: it helps us solve a problem related to dry run functionality that was hindered when a external Output was interwoven in the final\nfor-comprehension. External Outputs (Outputs that depend on real return values from Pulumi engine) no-op their flatMap/map chains in dry\nrun similarly to Option's None (because there is no value to feed to the passed function) and therefore led to exports code not being\nexecuted in dry run at all, causing a diff showing that all exports are going to be removed in preview and then recreated in apply phase.\nNew type of ",(0,i.yg)("inlineCode",{parentName:"p"},"Pulumi.run")," function disallows returning of async values - Stack has to be returned unwrapped, synchronously. Stack is just a\ncase class that takes only two arguments: ",(0,i.yg)("inlineCode",{parentName:"p"},"exports: Exports")," and ",(0,i.yg)("inlineCode",{parentName:"p"},"dependsOn: Vector[Output[?]]"),". ",(0,i.yg)("inlineCode",{parentName:"p"},"exports")," serve the same purpose as\nbefore and ",(0,i.yg)("inlineCode",{parentName:"p"},"dependsOn")," is used to ",(0,i.yg)("em",{parentName:"p"},"use")," all the ",(0,i.yg)("inlineCode",{parentName:"p"},"Outputs")," that have to be evaluated for this stack to be constructed but are not to be\nexported. You can return a ",(0,i.yg)("inlineCode",{parentName:"p"},"Stack")," that only consists of exports (for instance when everything you depend on is composed into a thing that\nyou export in the final step) using ",(0,i.yg)("inlineCode",{parentName:"p"},"Stack.export(x = a, y = b)")," or a ",(0,i.yg)("inlineCode",{parentName:"p"},"Stack")," that has only dependencies when you don't want to export\nanything using ",(0,i.yg)("inlineCode",{parentName:"p"},"Stack(x, y)"),". You can also use some resources and export others using ",(0,i.yg)("inlineCode",{parentName:"p"},"Stack(a, b).export(x = i, y = j)")," syntax.\nHere's an example use of Stack:"),(0,i.yg)("pre",{parentName:"li"},(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'@main def main = Pulumi.run {\n  val awsPolicy = aws.iam.Policy("my-policy",...)\n  val s3 = aws.s3.Bucket("my-bucket")\n  val logMessage = log.info("Creating your bucket!") // logs are values too!\n\n  Stack(logMessage, awsPolicy).exports(\n    url = s3.publicEndpoint\n  )\n}\n'))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Improved ",(0,i.yg)("inlineCode",{parentName:"p"},"Config")," ergonomy, by automatic secrets handling and using ",(0,i.yg)("inlineCode",{parentName:"p"},"Output[A]")," as the return value,\nand also adding a helpful error message when a key is missing ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/issues/204"},"#204"))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Overhauled ",(0,i.yg)("inlineCode",{parentName:"p"},"ResourceOptions")," ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/pull/355"},"#355")))),(0,i.yg)("h3",{id:"new-features"},"New Features"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"added support for Gradle and Maven to the Besom Scala language host ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/issues/303"},"#303"))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/resources/functions/"},"Provider functions")," like ",(0,i.yg)("inlineCode",{parentName:"p"},"aws.ec2.getAmi")," were added to Besom.\nThey allow you to use the Pulumi SDK to fetch data from the cloud provider and use it in your program. Here's an example of how to use them:"),(0,i.yg)("pre",{parentName:"li"},(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'@main def main = Pulumi.run {\n  val ami = ec2.getAmi(\n    ec2.GetAmiArgs(\n      filters = List(\n        GetAmiFilterArgs(\n          name = "name",\n          values = List("amzn2-ami-hvm-*-x86_64-ebs")\n        )\n      ),\n      owners = List("137112412989"), // Amazon\n      mostRecent = true\n    )\n  )\n  val server = ec2.Instance("web-server-www", \n    ec2.InstanceArgs(ami = ami.id,...)\n  )\n\n  Stack(server).exports(\n    ami = ami.id\n  )\n}\n'))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/stack/#stackreferences"},"Stack References")," were added to Besom. They allow you to use outputs from\nanother stack as inputs to the current stack. Here's an example of how to use them ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/pull/348"},"#348"),":"),(0,i.yg)("pre",{parentName:"li"},(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'@main def main = Pulumi.run {\n  ...\n  Stack.exports(\n    someOutput = "Hello world!",\n  )\n}\n')),(0,i.yg)("pre",{parentName:"li"},(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'@main def main = Pulumi.run {\n  val otherStack = besom.StackReference("stackRef", StackReferenceArgs("organization/source-stack-test/my-stack-name"))\n  val otherStackOutput = otherStack.output[String]("someOutput")\n  ...\n}\n'))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Added support for ",(0,i.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/resources/get/"},"Get functions")," to Besom. You can use the static get function,\nwhich is available on all resource types, to look up an existing resource that is not managed by Pulumi. Here's an example of how to use it:"),(0,i.yg)("pre",{parentName:"li"},(0,i.yg)("code",{parentName:"pre",className:"language-scala"},'@main def main = Pulumi.run {\n  val group = aws.ec2.SecurityGroup.get("group", "sg-0dfd33cdac25b1ec9")\n  ...\n}\n'))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Added support for ",(0,i.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/config/#structured-configuration"},"Structured Configuration"),", this allows user to\nread structured configuration from Pulumi configuration into JSON AST (",(0,i.yg)("inlineCode",{parentName:"p"},"config.getJson")," or ",(0,i.yg)("inlineCode",{parentName:"p"},"config.requireJson"),")\nor deserialize to an object (",(0,i.yg)("inlineCode",{parentName:"p"},"config.getObject")," or ",(0,i.yg)("inlineCode",{parentName:"p"},"config.requireObject"),") ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/issues/207"},"#207"))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Added new methods to the Besom ",(0,i.yg)("inlineCode",{parentName:"p"},"Context")," and ",(0,i.yg)("inlineCode",{parentName:"p"},"Resource"),", that allow for introspection into basic Pulumi metadata:\n",(0,i.yg)("inlineCode",{parentName:"p"},"pulumiResourceName")," and ",(0,i.yg)("inlineCode",{parentName:"p"},"pulumiProject"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"pulumiOrganization"),", ",(0,i.yg)("inlineCode",{parentName:"p"},"pulumiStack")," ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/issues/295"},"#295"))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Added support for ",(0,i.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/intro/concepts/programming-model/#remote-components"},"Remote Components")," to Besom\n",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/pull/355"},"#355"),".")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Provider SDKs Code Generator was improved on multiple fronts:"),(0,i.yg)("ul",{parentName:"li"},(0,i.yg)("li",{parentName:"ul"},"normalized the generated code to be more idiomatic and consistent with the rest of the SDK"),(0,i.yg)("li",{parentName:"ul"},"added support for provider functions and methods"),(0,i.yg)("li",{parentName:"ul"},"added support for component providers "),(0,i.yg)("li",{parentName:"ul"},"added support for convenient provider configuration access ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/259"},"#259")),(0,i.yg)("li",{parentName:"ul"},"allow to refer to other resources by reference instead of ID ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/144"},"#144")))),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},"Improved ","[NonEmptyString]"," inference via metaprogramming to find string literals by recursively traversing the AST tree. It now coveres cases where strings are defined directly as ",(0,i.yg)("inlineCode",{parentName:"p"},": String"),", concatenated with ",(0,i.yg)("inlineCode",{parentName:"p"},"+"),", multiplied with ",(0,i.yg)("inlineCode",{parentName:"p"},"*"),", all kinds of interpolators (including ",(0,i.yg)("inlineCode",{parentName:"p"},"p"),"/",(0,i.yg)("inlineCode",{parentName:"p"},"pulumi")," interpolator) are covered. Additionally, some new extension methods are defined to ease work with NES:"),(0,i.yg)("ul",{parentName:"li"},(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},'"string".toNonEmpty: Option[NonEmptyString]')," - safe, same as ",(0,i.yg)("inlineCode",{parentName:"li"},'NonEmptyString("string")')),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},'"string".toNonEmptyOrThrow: '),"NonEmptyString` - an unsafe extension for situations where you just can't be bothered"),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},'"string".toNonEmptyOutput: Output[NonEmptyString]')," - safe, if the string is empty the returned Output will be failed"),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("inlineCode",{parentName:"li"},'Output("string").toNonEmptyOutput: Output[NonEmptyString]')," - safe, if the string inside of the Output is empty the returned Output will be failed")))),(0,i.yg)("h3",{id:"bug-fixes-3"},"Bug Fixes"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"fixed logging via Pulumi RPC and added user-level MDC, now logs are properly displayed in the Pulumi console ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/199"},"#199")),(0,i.yg)("li",{parentName:"ul"},"fixed failing gRPC shutdown in our core SDK by correcting the lifecycle handling, now the SDK properly shuts down ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/228"},"#228")),(0,i.yg)("li",{parentName:"ul"},"fixed failing gRPC serialisation in multiple cases, now the SDK properly serialises and deserializes messages ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/148"},"#148")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/150"},"#150")),(0,i.yg)("li",{parentName:"ul"},"fixed failing shorthand config key names, now the SDK properly handles shorthand config key names ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/issues/205"},"#205")),(0,i.yg)("li",{parentName:"ul"},"fixed ",(0,i.yg)("inlineCode",{parentName:"li"},"Output.sequence"),"/",(0,i.yg)("inlineCode",{parentName:"li"},"Result.sequence")," multiple evaluations issue, now the SDK properly handles multiple evaluations of ",(0,i.yg)("inlineCode",{parentName:"li"},"Output.sequence"),"/",(0,i.yg)("inlineCode",{parentName:"li"},"Result.sequence")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/313"},"#313")),(0,i.yg)("li",{parentName:"ul"},"fixed failing codecs for provider inputs, now the SDK properly handles provider inputs ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/312"},"#312")),(0,i.yg)("li",{parentName:"ul"},"fixed ",(0,i.yg)("inlineCode",{parentName:"li"},"Output.traverse")," runtime exception, now the SDK properly handles ",(0,i.yg)("inlineCode",{parentName:"li"},"Output.traverse")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/360"},"#360")),(0,i.yg)("li",{parentName:"ul"},"fixed fatal errors hanging the SDK, now the SDK properly handles fatal errors ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/361"},"#361")),(0,i.yg)("li",{parentName:"ul"},"fixed resource decoders failing at runtime when handling special resources ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/364"},"#364")),(0,i.yg)("li",{parentName:"ul"},"fixed transitive dependency resolution algorithm, now the SDK properly resolves transitive dependencies ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/371"},"#371")),(0,i.yg)("li",{parentName:"ul"},"fixed failing code generation for unsupported or malformed schema files, now the generator properly handles schemas ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/265"},"#265")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/270"},"#270")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/274"},"#274")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/329"},"#329")," ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/332"},"#332")," "),(0,i.yg)("li",{parentName:"ul"},"fixed failing code generation when clashing names are present in the schema, now the generator properly handles clashing names ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/275"},"#275")),(0,i.yg)("li",{parentName:"ul"},"fixed failing code generation schema deserialization for complex types used as underlying types of named types ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/pull/282"},"#282"))),(0,i.yg)("h3",{id:"other-changes-2"},"Other Changes"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},"added more ",(0,i.yg)("a",{parentName:"li",href:"https://github.com/VirtusLab/besom/blob/release/v0.2.0/examples/README.md"},"examples")," to the Besom repository"),(0,i.yg)("li",{parentName:"ul"},"introduce schema-driven integration tests for ",(0,i.yg)("inlineCode",{parentName:"li"},"codegen")," fed from upstream Pulumi to improve reliability of the code generator"),(0,i.yg)("li",{parentName:"ul"},"many internal improvements and refactorings to the codebase to improve the overall quality of the SDK and its maintainability")),(0,i.yg)("p",null,(0,i.yg)("strong",{parentName:"p"},"Full Changelog"),": ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/compare/v0.1.0...v0.2.0"},"GitHub (v0.1.0...v0.2.0)")))}g.isMDXComponent=!0}}]);