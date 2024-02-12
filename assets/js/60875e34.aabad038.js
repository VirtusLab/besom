"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[400],{5788:(e,a,t)=>{t.d(a,{Iu:()=>u,yg:()=>g});var n=t(1504);function o(e,a,t){return a in e?Object.defineProperty(e,a,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[a]=t,e}function i(e,a){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);a&&(n=n.filter((function(a){return Object.getOwnPropertyDescriptor(e,a).enumerable}))),t.push.apply(t,n)}return t}function r(e){for(var a=1;a<arguments.length;a++){var t=null!=arguments[a]?arguments[a]:{};a%2?i(Object(t),!0).forEach((function(a){o(e,a,t[a])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):i(Object(t)).forEach((function(a){Object.defineProperty(e,a,Object.getOwnPropertyDescriptor(t,a))}))}return e}function s(e,a){if(null==e)return{};var t,n,o=function(e,a){if(null==e)return{};var t,n,o={},i=Object.keys(e);for(n=0;n<i.length;n++)t=i[n],a.indexOf(t)>=0||(o[t]=e[t]);return o}(e,a);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(n=0;n<i.length;n++)t=i[n],a.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(o[t]=e[t])}return o}var l=n.createContext({}),c=function(e){var a=n.useContext(l),t=a;return e&&(t="function"==typeof e?e(a):r(r({},a),e)),t},u=function(e){var a=c(e.components);return n.createElement(l.Provider,{value:a},e.children)},p="mdxType",d={inlineCode:"code",wrapper:function(e){var a=e.children;return n.createElement(n.Fragment,{},a)}},m=n.forwardRef((function(e,a){var t=e.components,o=e.mdxType,i=e.originalType,l=e.parentName,u=s(e,["components","mdxType","originalType","parentName"]),p=c(t),m=o,g=p["".concat(l,".").concat(m)]||p[m]||d[m]||i;return t?n.createElement(g,r(r({ref:a},u),{},{components:t})):n.createElement(g,r({ref:a},u))}));function g(e,a){var t=arguments,o=a&&a.mdxType;if("string"==typeof e||o){var i=t.length,r=new Array(i);r[0]=m;var s={};for(var l in a)hasOwnProperty.call(a,l)&&(s[l]=a[l]);s.originalType=e,s[p]="string"==typeof e?e:o,r[1]=s;for(var c=2;c<i;c++)r[c]=t[c];return n.createElement.apply(null,r)}return n.createElement.apply(null,t)}m.displayName="MDXCreateElement"},1304:(e,a,t)=>{t.r(a),t.d(a,{assets:()=>l,contentTitle:()=>r,default:()=>d,frontMatter:()=>i,metadata:()=>s,toc:()=>c});var n=t(5072),o=(t(1504),t(5788));const i={title:"Tutorial"},r=void 0,s={unversionedId:"tutorial",id:"tutorial",title:"Tutorial",description:"Introduction",source:"@site/docs/tutorial.md",sourceDirName:".",slug:"/tutorial",permalink:"/besom/docs/tutorial",draft:!1,tags:[],version:"current",frontMatter:{title:"Tutorial"},sidebar:"docsSidebar",previous:{title:"Getting started",permalink:"/besom/docs/getting_started"},next:{title:"Pulumi Basics",permalink:"/besom/docs/basics"}},l={},c=[{value:"Introduction",id:"introduction",level:2},{value:"Architecture",id:"architecture",level:2},{value:"Infrastructure-as-Scala-code",id:"infrastructure-as-scala-code",level:2},{value:"AWS S3 Bucket",id:"aws-s3-bucket",level:2},{value:"Resource names, identifiers and URNs",id:"resource-names-identifiers-and-urns",level:3},{value:"Create the resources stack",id:"create-the-resources-stack",level:3},{value:"Make the bucket public",id:"make-the-bucket-public",level:3},{value:"DynamoDB Table",id:"dynamodb-table",level:2},{value:"AWS Lambdas",id:"aws-lambdas",level:2},{value:"AWS API Gateway",id:"aws-api-gateway",level:2},{value:"Export stack outputs",id:"export-stack-outputs",level:2},{value:"Addendum A - debugging",id:"addendum-a---debugging",level:2},{value:"Addendum B - final <code>Stack</code> block",id:"addendum-b---final-stack-block",level:2},{value:"Addendum C - complete branch",id:"addendum-c---complete-branch",level:2}],u={toc:c},p="wrapper";function d(e){let{components:a,...i}=e;return(0,o.yg)(p,(0,n.c)({},u,i,{components:a,mdxType:"MDXLayout"}),(0,o.yg)("h2",{id:"introduction"},"Introduction"),(0,o.yg)("hr",null),(0,o.yg)("p",null,"Besom, the Pulumi SDK for Scala allows you to create, deploy and manage cloud resources such as databases, serverless\nfunctions and services quickly and safely. In this tutorial your will do exactly that - you will deploy a very basic\nyet functional serverless application built in Scala 3. The target environment will be\n",(0,o.yg)("a",{parentName:"p",href:"https://aws.amazon.com/"},"Amazon Web Services"),". Everything covered in this tutorial fits into the free tier so the only requirement is to\nactually have an AWS account. You can ",(0,o.yg)("a",{parentName:"p",href:"https://portal.aws.amazon.com/billing/signup"},"create one here")," if you don't have one."),(0,o.yg)("p",null,"To start first install all the necessary tools mentioned in ",(0,o.yg)("a",{parentName:"p",href:"/besom/docs/getting_started"},"Getting started")," section."),(0,o.yg)("p",null,"You will also need to obtain ",(0,o.yg)("strong",{parentName:"p"},"AWS Access Key ID")," and ",(0,o.yg)("strong",{parentName:"p"},"AWS Secret Access Key")," from IAM Console. Once you have them,\ninstall ",(0,o.yg)("a",{parentName:"p",href:"https://aws.amazon.com/cli/"},"the AWS CLI")," for your platform and perform "),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-bash"},"aws configure\n")),(0,o.yg)("p",null,"This will set up your AWS access for use with Pulumi."),(0,o.yg)("admonition",{type:"caution"},(0,o.yg)("p",{parentName:"admonition"},"It's ",(0,o.yg)("strong",{parentName:"p"},"strongly")," recommended that you use an IAM account with ",(0,o.yg)("strong",{parentName:"p"},"AdministratorAccess")," permissions as it guarantees that\nyou won't encounter issues related to missing permissions. Additionally, it's also recommended to set your\n",(0,o.yg)("a",{parentName:"p",href:"https://docs.aws.amazon.com/awsconsolehelpdocs/latest/gsg/select-region.html"},"default region")," in AWS Console.")),(0,o.yg)("admonition",{type:"tip"},(0,o.yg)("p",{parentName:"admonition"},"After running ",(0,o.yg)("inlineCode",{parentName:"p"},"aws configure")," it's a good idea to run: "),(0,o.yg)("pre",{parentName:"admonition"},(0,o.yg)("code",{parentName:"pre",className:"language-bash"},"aws sts get-caller-identity\n")),(0,o.yg)("p",{parentName:"admonition"},"This allows you to check that everything is fine! "),(0,o.yg)("p",{parentName:"admonition"},"If it is you should see a JSON containing your user id, AWS account number and\nyour user's ARN.")),(0,o.yg)("p",null,"After all of that is done the last step is to clone the ",(0,o.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom-tutorial"},"tutorial repository"),":"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-bash"},"git clone git@github.com:VirtusLab/besom-tutorial.git && cd besom-tutorial\n")),(0,o.yg)("p",null,"Repository contains the sources for AWS Lambda app built with Scala 3. The application itself is already prepared for you,\nyou don't have to write it from scratch. Moreover, the app is built with GraalVM Native-Image so the build result is a\nnative binary allowing for very fast cold starts and miniscule resource use. "),(0,o.yg)("p",null,"Sources of the app reside in the ",(0,o.yg)("inlineCode",{parentName:"p"},"./lambda")," directory."),(0,o.yg)("p",null,"You can build the application yourself using the provided ",(0,o.yg)("inlineCode",{parentName:"p"},"./build.sh")," script ",(0,o.yg)("strong",{parentName:"p"},"if you're running on Linux with AMD64\nprocessor architecture"),". Unfortunately GraalVM does not support compilation of native images for architectures different\nthan the one that you are using. For all users on Macs and Windows (and Linux users on ARM platforms) we have provided pre-built artifacts,\nalready packaged into zip files."),(0,o.yg)("p",null,"These artifacts can be found in ",(0,o.yg)("inlineCode",{parentName:"p"},"./pre-built/")," directory of the repository."),(0,o.yg)("h2",{id:"architecture"},"Architecture"),(0,o.yg)("hr",null),(0,o.yg)("p",null,"The application you are going to deploy to AWS is called CatPost. It's a simple app from simpler times - its only\nfunctionality is for the cat lovers to post pictures of their cats along with their names and comments regarding\nthe picture."),(0,o.yg)("p",null,"Here's a chart describing the infrastructure of the application:"),(0,o.yg)("p",null,(0,o.yg)("img",{alt:"Tutorial app architecture",src:t(592).c,width:"1231",height:"720"})),(0,o.yg)("p",null,"The app consists of two AWS Lambdas reachable on a public endpoint of Amazon API Gateway. The data about posts is held\nin a DynamoDB table and the pictures of cats are stored in a publicly available AWS S3 bucket. First lambda renders the\npage with post feed, second handles creation of new posts, uploads data to S3 and DynamoDB.\nOk, let's deploy!"),(0,o.yg)("h2",{id:"infrastructure-as-scala-code"},"Infrastructure-as-Scala-code"),(0,o.yg)("hr",null),(0,o.yg)("p",null,"Your infrastructure will live inside the ",(0,o.yg)("inlineCode",{parentName:"p"},"./besom")," directory. Once you open the directory in your IDE (remember:\n",(0,o.yg)("inlineCode",{parentName:"p"},"scala-cli setup-ide ."),") you will notice that there are 3 files there:"),(0,o.yg)("ul",null,(0,o.yg)("li",{parentName:"ul"},(0,o.yg)("inlineCode",{parentName:"li"},"project.scala")," - ",(0,o.yg)("a",{parentName:"li",href:"https://scala-cli.virtuslab.org/docs/guides/using-directives/"},"Scala-CLI project definition")),(0,o.yg)("li",{parentName:"ul"},(0,o.yg)("inlineCode",{parentName:"li"},"Pulumi.yaml")," - ",(0,o.yg)("a",{parentName:"li",href:"https://www.pulumi.com/docs/concepts/projects/project-file/"},"Pulumi project definition")),(0,o.yg)("li",{parentName:"ul"},(0,o.yg)("inlineCode",{parentName:"li"},"Main.scala")," - the actual Pulumi program written in Scala using Besom")),(0,o.yg)("p",null,"Go ahead and open the ",(0,o.yg)("inlineCode",{parentName:"p"},"Main.scala")," file. Inside you will see this snippet:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"import besom.*\n\n@main def main: Unit = Pulumi.run {\n  val warning = log.warn(\"Nothing's here yet, it's waiting for you to write some code!\")\n  \n  Stack(warning)\n}\n")),(0,o.yg)("p",null,"To learn more about projects and programs in Besom refer to the ",(0,o.yg)("a",{parentName:"p",href:"/besom/docs/basics#projects"},"Projects section of the Basics page"),"."),(0,o.yg)("p",null,"This is the minimal Besom program that doesn't do anything beside logging a message. Let's change that and add some\nAWS components! "),(0,o.yg)("h2",{id:"aws-s3-bucket"},"AWS S3 Bucket"),(0,o.yg)("hr",null),(0,o.yg)("p",null,"The first step that you are going to do is to set up data storage for our app. The most important part of application\nfor cat lovers is a place to store pictures of cats, so we'll start with an AWS S3 bucket. AWS S3 is a data storage\nservice that allows users to perform all operations using HTTP interface. This is great for this app because you can\njust use the bucket as a file server that will serve pictures of cats to whomever requires them. Let's start with\nthe definition of a bucket:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'import besom.api.aws.* // place this on top of the file\n\n// place this inside of the `Pulumi.run` scope\nval feedBucket = s3.Bucket(\n  "pulumi-catpost-cat-pics",\n  s3.BucketArgs(\n    forceDestroy = true // without this Pulumi won\'t delete the bucket on destroy\n  )\n)\n')),(0,o.yg)("p",null,"You still need to add this resource to program's main flow so change this:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"val warning = log.warn(\"Nothing's here yet, it's waiting for you to write some code!\")\n\nStack(warning)\n")),(0,o.yg)("p",null,"into this:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"Stack(feedBucket)\n")),(0,o.yg)("p",null,"Simple enough. Notice that what is done here is that you provide a name for Pulumi resource, not a name for the bucket\nitself! You could provide a name for this bucket like this:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val feedBucket = s3.Bucket(\n  "pulumi-catpost-cat-pics",\n  s3.BucketArgs(\n    bucket = "pulumi-catpost-cat-pics"\n  )\n)\n')),(0,o.yg)("h3",{id:"resource-names-identifiers-and-urns"},"Resource names, identifiers and URNs"),(0,o.yg)("p",null,"Since bucket names in AWS are ",(0,o.yg)("strong",{parentName:"p"},"globally unique"),", it would be very easy to have a name clash with\nanother user. When you omit the explicit name of a resource via it's resource arguments Pulumi uses the name of Pulumi\nresource and suffixes it with a random string to generate a unique name (auto-name). This distinction is quite important\nbecause Pulumi resource name is a part of ",(0,o.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/resources/names/"},"Pulumi URN"),", but the actual bucket name is a part of its resource ID in AWS infrastructure.\nTo learn more please refer to the ",(0,o.yg)("a",{parentName:"p",href:"/besom/docs/basics#resources"},"Resources section of the Basics page"),"."),(0,o.yg)("p",null,"For now let's just remember that first\nargument of Besom resource constructor function is always the Pulumi resource name and resource names for cloud\nprovider to use are always provided via resource arguments."),(0,o.yg)("h3",{id:"create-the-resources-stack"},"Create the resources stack"),(0,o.yg)("p",null,"You can try and create your bucket now. To do this execute ",(0,o.yg)("inlineCode",{parentName:"p"},"pulumi up")," in ",(0,o.yg)("inlineCode",{parentName:"p"},"./besom")," directory: "),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-bash"},"cd besom\npulumi up\n")),(0,o.yg)("p",null,"Pulumi will ask you to create a ",(0,o.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/stack/"},"stack"),". Name it however you want, we named it ",(0,o.yg)("inlineCode",{parentName:"p"},"dev")," because\nstacks commonly match application environments. You might be also asked to provide a password for this stack. This is\nactually quite important because Pulumi uses that password to encrypt sensitive data related to your deployment. As\nyou are just playing you can use an empty password, but it's ",(0,o.yg)("em",{parentName:"p"},"very important")," to use a proper password for actual work.\nPulumi will subsequently print a preview of changes between current state of affairs and the changes your code will\nintroduce to the cloud environment. To learn more about stacks refer to the ",(0,o.yg)("a",{parentName:"p",href:"/besom/docs/basics#stacks"},"Stacks section of the Basics page")),(0,o.yg)("p",null,"Select ",(0,o.yg)("inlineCode",{parentName:"p"},"yes")," when asked whether you want to deploy this set of changes to cloud."),(0,o.yg)("p",null,"To work faster you can also use these shorthand commands:"),(0,o.yg)("ul",null,(0,o.yg)("li",{parentName:"ul"},(0,o.yg)("inlineCode",{parentName:"li"},"pulumi up --stack dev -y")," - tells Pulumi to use stack named ",(0,o.yg)("inlineCode",{parentName:"li"},"dev")," and not to ask for confirmation of deployment\nafter preview"),(0,o.yg)("li",{parentName:"ul"},(0,o.yg)("inlineCode",{parentName:"li"},"pulumi destroy --stack dev -y")," - tells Pulumi to ",(0,o.yg)("strong",{parentName:"li"},"destroy")," the infrastructure in stack named ",(0,o.yg)("inlineCode",{parentName:"li"},"dev")," without asking\nfor confirmation")),(0,o.yg)("p",null,"Remember that in actual production environment you can't just ",(0,o.yg)("inlineCode",{parentName:"p"},"pulumi destroy")," things. Since we're just trying things\nout it will be handy however because it's a tutorial. If you get into any trouble you can just start over by\ndestroying everything and then applying your program from ground up."),(0,o.yg)("h3",{id:"make-the-bucket-public"},"Make the bucket public"),(0,o.yg)("p",null,"There's one huge problem with our bucket now - all S3 buckets are private by default. Let's make it public. "),(0,o.yg)("p",null,"Making an S3 bucket public is quite involved because turning a private bucket public can deal an enormous amount of damage\nto a company. To achieve this you will need to create two new resources - a ",(0,o.yg)("inlineCode",{parentName:"p"},"BucketPublicAccessBlock")," and a ",(0,o.yg)("inlineCode",{parentName:"p"},"BucketPolicy"),".\nThis will also allow us to introduce two new concepts."),(0,o.yg)("p",null,"Here's the code necessary to create a ",(0,o.yg)("inlineCode",{parentName:"p"},"BucketPublicAccessBlock"),":"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val bucketName = feedBucket.bucket\n\nval feedBucketPublicAccessBlock = bucketName.flatMap { name =>\n  s3.BucketPublicAccessBlock(\n    s"${name}-publicaccessblock",\n    s3.BucketPublicAccessBlockArgs(\n      bucket = feedBucket.id,\n      blockPublicPolicy = false // Do not block public bucket policies for this bucket\n    )\n  )\n}\n')),(0,o.yg)("p",null,"First new thing is that the ",(0,o.yg)("inlineCode",{parentName:"p"},"bucket")," property of a bucket is used here via ",(0,o.yg)("a",{parentName:"p",href:"/besom/docs/lifting"},"lifting"),". You need the name\nof the bucket that was actually created in AWS because it's a unique name generated in runtime. Next step is to use\nthat name by ",(0,o.yg)("inlineCode",{parentName:"p"},"flatMap"),"ping on the ",(0,o.yg)("inlineCode",{parentName:"p"},"Output[String]")," value of the ",(0,o.yg)("inlineCode",{parentName:"p"},"bucket")," property to name the ",(0,o.yg)("inlineCode",{parentName:"p"},"BucketPublicAccessBlock")," resource via string interpolation. Then the actual cloud identifier is passed via ",(0,o.yg)("inlineCode",{parentName:"p"},"BucketPublicAccessBlockArgs")," to let\nAWS know which bucket this public access block regards. Finally ",(0,o.yg)("inlineCode",{parentName:"p"},"blockPublicPolicy")," property is set to ",(0,o.yg)("inlineCode",{parentName:"p"},"false")," to allow\nfor assignment of the ",(0,o.yg)("inlineCode",{parentName:"p"},"PublicReadGetObject")," policy. Speaking of policy - here's the next snippet:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'import spray.json.* // place this on top of the file\n\nval feedBucketPolicy = bucketName.flatMap(name =>\n  s3.BucketPolicy(\n    s"${name}-access-policy",\n    s3.BucketPolicyArgs(\n      bucket = feedBucket.id,\n      policy = JsObject(\n        "Version" -> JsString("2012-10-17"),\n        "Statement" -> JsArray(\n          JsObject(\n            "Sid" -> JsString("PublicReadGetObject"),\n            "Effect" -> JsString("Allow"),\n            "Principal" -> JsObject(\n              "AWS" -> JsString("*")\n            ),\n            "Action" -> JsArray(JsString("s3:GetObject")),\n            "Resource" -> JsArray(JsString(s"arn:aws:s3:::${name}/*"))\n          )\n        )\n      ).prettyPrint\n    ),\n    opts(\n      dependsOn = feedBucketPublicAccessBlock\n    )\n  )\n)\n')),(0,o.yg)("p",null,"First of all, the dynamically generated unique bucket name is used again to name this resource too, and then\nits ID is used once more to inform ",(0,o.yg)("inlineCode",{parentName:"p"},"BucketPolicy")," about the bucket it should be applied to. "),(0,o.yg)("p",null,"The policy itself is expressed here using JSON AST of ",(0,o.yg)("a",{parentName:"p",href:"https://github.com/spray/spray-json"},"spray-json")," library and rendered to string.\nAlternatively, it could be just an interpolated string literal, it could also be loaded from a file\nand interpolated using regular expression. "),(0,o.yg)("p",null,"The last new step is the use of ",(0,o.yg)("inlineCode",{parentName:"p"},"dependsOn")," property of ",(0,o.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/options/"},"resource options"),". We use it here to inform Pulumi about the\nnecessity of ordered creation of resources - the ",(0,o.yg)("inlineCode",{parentName:"p"},"BucketPublicAccessBlock")," with ",(0,o.yg)("inlineCode",{parentName:"p"},"blockPublicPolicy = false"),"\nhas to be created before a public policy is actually applied to the bucket. Should we allow Pulumi to run\neverything in parallel AWS API would return a ",(0,o.yg)("inlineCode",{parentName:"p"},"403 Forbidden")," error. More about this topic can be found in\n",(0,o.yg)("a",{parentName:"p",href:"/besom/docs/constructors"},"Resource constructors, outputs and asynchronicity")," section of the docs."),(0,o.yg)("p",null,"Please don't forget to add both resources to your ",(0,o.yg)("inlineCode",{parentName:"p"},"Stack"),":"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"Stack(feedBucket, feedBucketPublicAccessBlock, feedBucketPolicy)\n")),(0,o.yg)("p",null,"If you run your program now you will have an empty but publicly accessible S3 bucket!"),(0,o.yg)("h2",{id:"dynamodb-table"},"DynamoDB Table"),(0,o.yg)("hr",null),(0,o.yg)("p",null,"In scope of the lambda app at ",(0,o.yg)("inlineCode",{parentName:"p"},"./lambda/dynamodb.scala")," you can find the case class used to model a row of\ndata in AWS DynamoDB. It looks like this:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"case class CatPost(\n  entryId: String,\n  userName: String,\n  comment: String,\n  timestamp: String,\n  catPictureURL: String\n)\n")),(0,o.yg)("p",null,(0,o.yg)("a",{parentName:"p",href:"https://aws.amazon.com/dynamodb/"},"DynamoDB")," is a NoSQL database so there's no column schema so to speak to create.\nCatPost is however in need of a way to sort the entries to display them in correct order - from oldest to newest.\nThis means that DynamoDB table will have to have both a hash key and range key that will use the ",(0,o.yg)("inlineCode",{parentName:"p"},"timestamp")," attribute. "),(0,o.yg)("p",null,"Here's how you can define such table using Besom:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'import besom.api.aws.dynamodb.inputs.* // place this on top of the file\n\nval catPostTable = dynamodb.Table(\n  "pulumi-catpost-table",\n  dynamodb.TableArgs(\n    attributes = List(\n      TableAttributeArgs(\n        name = "PartitionKey",\n        `type` = "S"\n      ),\n      TableAttributeArgs(\n        name = "timestamp",\n        `type` = "S"\n      )\n    ),\n    hashKey = "PartitionKey",\n    rangeKey = "timestamp",\n    readCapacity = 5,\n    writeCapacity = 5\n  )\n)\n')),(0,o.yg)("p",null,"That's all! DynamoDB is that easy to set up. Add the resource to the final ",(0,o.yg)("inlineCode",{parentName:"p"},"Stack")," and run it."),(0,o.yg)("h2",{id:"aws-lambdas"},"AWS Lambdas"),(0,o.yg)("p",null,"Ok, now you're getting to the point. You will now deploy both ",(0,o.yg)("a",{parentName:"p",href:"https://aws.amazon.com/lambda/"},"AWS Lambdas"),".\nFor lambdas to run they have to have a correct set of permissions aggregated as an AWS IAM Role.\nLet's start with creation of a Role that will allow lambdas to manipulate DynamoDB, S3 and run in Lambda runtime:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val lambdaRole = iam.Role("lambda-role", iam.RoleArgs(\n  assumeRolePolicy = JsObject(\n    "Version" -> JsString("2012-10-17"),\n    "Statement" -> JsArray(\n      JsObject(\n        "Action" -> JsString("sts:AssumeRole"),\n        "Effect" -> JsString("Allow"),\n        "Principal" -> JsObject(\n          "Service" -> JsString("lambda.amazonaws.com")\n        )\n      )\n    )\n  ).prettyPrint,\n  forceDetachPolicies = true, // just so that pulumi destroy works correctly\n  managedPolicyArns = List(\n    "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",\n    "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess",\n    "arn:aws:iam::aws:policy/AmazonS3FullAccess",\n  )\n))\n')),(0,o.yg)("p",null,"Now that there's a Role it's time to deploy the Lambdas:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'import besom.api.aws.lambda.inputs.* // place this on top of the file\nimport besom.types.Archive.FileArchive // place this on top of the file\n\nval stageName: NonEmptyString = "default"\n\nval feedLambda = lambda.Function(\n  "pulumi-render-feed",\n  lambda.FunctionArgs(\n    role = lambdaRole.arn,\n    runtime = "provided.al2", // Use the custom runtime\n    code = FileArchive("../pre-built/render-feed.zip"),\n    handler = "whatever", // we don\'t use the handler, but it\'s required\n    environment = FunctionEnvironmentArgs(\n      variables = Map(\n        "STAGE" -> stageName,\n        "BUCKET_NAME" -> bucketName,\n        "DYNAMO_TABLE" -> catPostTable.name\n      )\n    )\n  )\n)\n\nval addLambda = lambda.Function(\n  "pulumi-add-post",\n  lambda.FunctionArgs(\n    role = lambdaRole.arn,\n    runtime = "provided.al2", // Use the custom runtime\n    code = FileArchive("../pre-built/post-cat-entry.zip"),\n    handler = "whatever", // we don\'t use the handler, but it\'s required\n    environment = FunctionEnvironmentArgs(\n      variables = Map(\n        "STAGE" -> stageName,\n        "BUCKET_NAME" -> bucketName,\n        "DYNAMO_TABLE" -> catPostTable.name\n      )\n    )\n  )\n)\n')),(0,o.yg)("p",null,"Let's talk about what've just happened. First thing is the stage name. This parameter is actually required later by the\nAWS API Gateway, but it's defined here so that self-URLs can be properly resolved in Lambdas.\nWe pass the stage name to the lambda via environment variables.\nLambda definitions take few arguments: "),(0,o.yg)("ul",null,(0,o.yg)("li",{parentName:"ul"},"a reference to the Role created above (",(0,o.yg)("inlineCode",{parentName:"li"},"lambdaRole"),"), "),(0,o.yg)("li",{parentName:"ul"},"a parameter defining which Lambda runtime should be used - we're using ",(0,o.yg)("a",{parentName:"li",href:"https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html#runtimes-custom-use"},"the new custom runtime"),"\nfor our GraalVM native binary, a set of environment variables to configure the app"),(0,o.yg)("li",{parentName:"ul"},"and a ",(0,o.yg)("inlineCode",{parentName:"li"},"FileArchive")," pointing to where the prepared Zip package with the native binary of Lambda exists.\nThe path is relative and points to pre-built packages by default.")),(0,o.yg)("p",null,"If you chose to rebuild the lambdas on your own you have to adjust the path so that it points to the relevant packages."),(0,o.yg)("p",null,"Add this to your program, add both lambdas to the ",(0,o.yg)("inlineCode",{parentName:"p"},"Stack")," at the end of your program, run ",(0,o.yg)("inlineCode",{parentName:"p"},"pulumi up")," and that's it, AWS Lambdas deployed."),(0,o.yg)("h2",{id:"aws-api-gateway"},"AWS API Gateway"),(0,o.yg)("p",null,"Last thing to do - you have to make your app accessible from the Internet. To do this you have to deploy\n",(0,o.yg)("a",{parentName:"p",href:"https://aws.amazon.com/api-gateway/"},"API Gateway")," with a correct configuration. "),(0,o.yg)("p",null,"Let's start with the API itself:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'import besom.api.aws.apigateway.inputs.* // place this on top of the file\n\nval api = apigateway.RestApi(\n  "api",\n  apigateway.RestApiArgs(\n    binaryMediaTypes = List("multipart/form-data"),\n    endpointConfiguration = RestApiEndpointConfigurationArgs(types = "REGIONAL")\n  )\n)\n')),(0,o.yg)("p",null,"This creates an API that will be able to properly transmit multipart HTTP requests (cat pictures!) to the ",(0,o.yg)("inlineCode",{parentName:"p"},"addPost")," lambda. "),(0,o.yg)("p",null,"Next, the API needs permissions to invoke the lambdas:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val feedLambdaPermission = lambda.Permission(\n  "feedLambdaPermission",\n  lambda.PermissionArgs(\n    action = "lambda:InvokeFunction",\n    function = feedLambda.name,\n    principal = "apigateway.amazonaws.com",\n    sourceArn = p"${api.executionArn}/*"\n  )\n)\n\nval addLambdaPermission = lambda.Permission(\n  "addLambdaPermission",\n  lambda.PermissionArgs(\n    action = "lambda:InvokeFunction",\n    function = addLambda.name,\n    principal = "apigateway.amazonaws.com",\n    sourceArn = p"${api.executionArn}/*"\n  )\n)\n')),(0,o.yg)("p",null,"Next - routing configuration for the API:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val feedMethod = apigateway.Method(\n  "feedMethod",\n  apigateway.MethodArgs(\n    restApi = api.id,\n    resourceId = api.rootResourceId,\n    httpMethod = "GET",\n    authorization = "NONE"\n  )\n)\n\nval addResource = apigateway.Resource(\n  "addResource",\n  apigateway.ResourceArgs(\n    restApi = api.id,\n    pathPart = "post",\n    parentId = api.rootResourceId\n  )\n)\n\nval addMethod = apigateway.Method(\n  "addMethod",\n  apigateway.MethodArgs(\n    restApi = api.id,\n    resourceId = addResource.id,\n    httpMethod = "POST",\n    authorization = "NONE"\n  )\n)\n')),(0,o.yg)("p",null,"Now you have to configure the API Gateway to integrate with AWS Lambda properly:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val feedIntegration = apigateway.Integration(\n  "feedIntegration",\n  apigateway.IntegrationArgs(\n    restApi = api.id,\n    resourceId = api.rootResourceId, // use / path\n    httpMethod = feedMethod.httpMethod,\n    // must be POST, this is not a mistake: https://repost.aws/knowledge-center/api-gateway-lambda-template-invoke-error\n    integrationHttpMethod = "POST",\n    `type` = "AWS_PROXY", // Lambda Proxy integration\n    uri = feedLambda.invokeArn\n  )\n)\n\nval addIntegration = apigateway.Integration(\n  "addIntegration",\n  apigateway.IntegrationArgs(\n    restApi = api.id,\n    resourceId = addResource.id, // use /post path\n    httpMethod = addMethod.httpMethod,\n    // must be POST, this is not a mistake: https://repost.aws/knowledge-center/api-gateway-lambda-template-invoke-error\n    integrationHttpMethod = "POST",\n    `type` = "AWS_PROXY", // Lambda Proxy integration\n    uri = addLambda.invokeArn\n  )\n)\n')),(0,o.yg)("p",null,"Now we have one more thing to do, create API Gateway account settings (region wide) and associate it with the API:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val cloudwatchRole = iam.Role(\n  "cloudwatchRole",\n  iam.RoleArgs(\n    assumeRolePolicy = """{\n        |  "Version": "2012-10-17",\n        |  "Statement": [\n        |    {\n        |      "Effect": "Allow",\n        |      "Action": "sts:AssumeRole",\n        |      "Principal": {\n        |        "Service": [\n        |           "apigateway.amazonaws.com"\n        |        ]\n        |      }\n        |    }\n        |  ]\n        |}\n        |""".stripMargin.parseJson.prettyPrint,\n    managedPolicyArns = List(\n      "arn:aws:iam::aws:policy/service-role/AmazonAPIGatewayPushToCloudWatchLogs"\n    )\n  )\n)\n\nval cloudwatchRolePolicy = iam.RolePolicy(\n  "cloudwatchRolePolicy",\n  iam.RolePolicyArgs(\n    role = cloudwatchRole.id,\n    policy = """{\n        |  "Version": "2012-10-17",\n        |  "Statement": [\n        |    {\n        |      "Effect": "Allow",\n        |      "Action": [\n        |        "logs:CreateLogGroup",\n        |        "logs:CreateLogStream",\n        |        "logs:DescribeLogGroups",\n        |        "logs:DescribeLogStreams",\n        |        "logs:PutLogEvents",\n        |        "logs:GetLogEvents",\n        |        "logs:FilterLogEvents"\n        |      ],\n        |      "Resource": "*"\n        |    }\n        |  ]\n        |}\n        |""".stripMargin.parseJson.prettyPrint\n  )\n)\n\nval apiAccount = apigateway.Account(\n  "apiAccount",\n  apigateway.AccountArgs(\n    cloudwatchRoleArn = cloudwatchRole.arn\n  )\n)\n')),(0,o.yg)("admonition",{type:"caution"},(0,o.yg)("p",{parentName:"admonition"},"As there is no API method for deleting account settings or resetting it to defaults,\ndestroying this resource will keep your account settings intact!")),(0,o.yg)("p",null,"Finally, you have to create a deployment of the API with its stage (here's where we reuse the ",(0,o.yg)("inlineCode",{parentName:"p"},"stageName")," value from\nLambdas section):"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val apiDeployment = apigateway.Deployment(\n  "apiDeployment",\n  apigateway.DeploymentArgs(\n    restApi = api.id,\n    // workarounds for the underlying provider bugs\n    triggers = Map(\n      "resourceId" -> api.rootResourceId,\n      "feedMethodId" -> feedMethod.id,\n      "feedIntegrationId" -> feedIntegration.id,\n      "addResourceId" -> addResource.id,\n      "addMethodId" -> addMethod.id,\n      "addIntegrationId" -> addIntegration.id,\n    )\n  ),\n  opts(\n    dependsOn = List(feedLambda, addLambda),\n    deleteBeforeReplace = false\n  )\n)\n\nval apiStage = apigateway.Stage(\n  "apiStage",\n  apigateway.StageArgs(\n    restApi = api.id,\n    deployment = apiDeployment.id,\n    stageName = stageName\n  ),\n  opts(\n    deleteBeforeReplace = true\n  )\n)\n\nval apiStageSettings = apigateway.MethodSettings(\n  "apiStageSettings",\n  apigateway.MethodSettingsArgs(\n    restApi = api.id,\n    stageName = apiStage.stageName,\n    methodPath = "*/*",\n    settings = MethodSettingsSettingsArgs(\n      metricsEnabled = true,\n      loggingLevel = "ERROR"\n    )\n  ),\n  opts(\n    dependsOn = apiAccount\n  )\n)\n')),(0,o.yg)("p",null,"Two things that need attention here is that API deployment has to be sequenced with Lambdas and IAM creation so the\n",(0,o.yg)("inlineCode",{parentName:"p"},"dependsOn")," property is used again. Another thing is that\n",(0,o.yg)("a",{parentName:"p",href:"https://www.pulumi.com/docs/concepts/options/deletebeforereplace/"},(0,o.yg)("inlineCode",{parentName:"a"},"deleteBeforeReplace")," resource option")," makes an appearance.\nThese are necessary for AWS to correctly handle the deployment of these resources."),(0,o.yg)("h2",{id:"export-stack-outputs"},"Export stack outputs"),(0,o.yg)("p",null,"Ok, that's it. Add ",(0,o.yg)("em",{parentName:"p"},"all")," of these resources to Stack and then modify\nthe ",(0,o.yg)("a",{parentName:"p",href:"/besom/docs/exports"},(0,o.yg)("inlineCode",{parentName:"a"},"exports"))," so that it matches this:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"Stack(...).exports(\n  feedBucket = feedBucket.bucket,\n  endpointURL = apiStage.invokeUrl\n)\n")),(0,o.yg)("p",null,"This allows you to see both the dynamic, partially random name of the S3 bucket that has been created and also\nthe public URL under which your CatPost instance is available. "),(0,o.yg)("p",null,"You can access your stack outputs at any time using ",(0,o.yg)("inlineCode",{parentName:"p"},"pulumi stack output")," command, e.g.:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre"},"Current stack outputs (2):\n    OUTPUT       VALUE\n    endpointURL  https://***.execute-api.eu-central-1.amazonaws.com/default\n    feedBucket   pulumi-catpost-cat-pics-***\n")),(0,o.yg)("p",null,(0,o.yg)("strong",{parentName:"p"},"Visit it now and post some cats!")),(0,o.yg)("br",null),(0,o.yg)("br",null),(0,o.yg)("h2",{id:"addendum-a---debugging"},"Addendum A - debugging"),(0,o.yg)("p",null,"If you run into problems with CatPost lambdas when hacking on this tutorial you might want to create Cloudwatch\n",(0,o.yg)("inlineCode",{parentName:"p"},"LogGroup"),"s for them so that you can track what's in their logs:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'val feedLambdaLogs = feedLambda.name.flatMap { feedName => \n  val feedLambdaLogsName: NonEmptyString = s"/aws/lambda/$feedName"\n\n  cloudwatch.LogGroup(\n    feedLambdaLogsName,\n    cloudwatch.LogGroupArgs(\n      name = feedLambdaLogsName,\n      retentionInDays = 3\n    )\n  )\n}\n\nval addLambdaLogs = addLambda.name.flatMap { addName => \n  val addLambdaLogsName: NonEmptyString = s"/aws/lambda/$addName"\n\n  cloudwatch.LogGroup(\n    addLambdaLogsName,\n    cloudwatch.LogGroupArgs(\n      name = addLambdaLogsName,\n      retentionInDays = 3\n    )\n  )\n}\n')),(0,o.yg)("p",null,"Again, remember to add them to the Stack at the end of the program."),(0,o.yg)("h2",{id:"addendum-b---final-stack-block"},"Addendum B - final ",(0,o.yg)("inlineCode",{parentName:"h2"},"Stack")," block"),(0,o.yg)("p",null,"Here's how the final ",(0,o.yg)("inlineCode",{parentName:"p"},"Stack")," block looks like in a complete deployment:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"Stack(\n  feedBucket, \n  feedBucketPolicy, \n  feedBucketPublicAccessBlock,\n  catPostTable,\n  feedLambdaLogs, \n  addLambdaLogs,\n  feedLambda,\n  addLambda,\n  api,\n  feedLambdaPermission,\n  addLambdaPermission,\n  feedMethod,\n  addResource,\n  addMethod,\n  feedIntegration,\n  addIntegration,\n  apiDeployment,\n  apiStage,\n  apiAccount,\n  apiStageSettings,\n  cloudwatchRolePolicy\n).exports(\n  feedBucket = feedBucket.bucket,\n  endpointURL = apiStage.invokeUrl\n)\n")),(0,o.yg)("h2",{id:"addendum-c---complete-branch"},"Addendum C - complete branch"),(0,o.yg)("p",null,"If you run into problems during your tutorial run be sure to check out the ",(0,o.yg)("inlineCode",{parentName:"p"},"complete")," branch of the\n",(0,o.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom-tutorial"},(0,o.yg)("inlineCode",{parentName:"a"},"besom-tutorial")," repo"),".\nIt contains a complete, tested draft of the infrastructure you are tasked to prepare here."))}d.isMDXComponent=!0},592:(e,a,t)=>{t.d(a,{c:()=>n});const n=t.p+"assets/images/tutorial-arch-c73c7a159113763ea5fa346f263c7af1.png"}}]);