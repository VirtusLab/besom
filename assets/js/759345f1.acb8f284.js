"use strict";(self.webpackChunkbesom_website=self.webpackChunkbesom_website||[]).push([[481],{5680:(e,a,t)=>{t.d(a,{xA:()=>c,yg:()=>g});var n=t(6540);function r(e,a,t){return a in e?Object.defineProperty(e,a,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[a]=t,e}function i(e,a){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);a&&(n=n.filter((function(a){return Object.getOwnPropertyDescriptor(e,a).enumerable}))),t.push.apply(t,n)}return t}function o(e){for(var a=1;a<arguments.length;a++){var t=null!=arguments[a]?arguments[a]:{};a%2?i(Object(t),!0).forEach((function(a){r(e,a,t[a])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):i(Object(t)).forEach((function(a){Object.defineProperty(e,a,Object.getOwnPropertyDescriptor(t,a))}))}return e}function l(e,a){if(null==e)return{};var t,n,r=function(e,a){if(null==e)return{};var t,n,r={},i=Object.keys(e);for(n=0;n<i.length;n++)t=i[n],a.indexOf(t)>=0||(r[t]=e[t]);return r}(e,a);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(n=0;n<i.length;n++)t=i[n],a.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(r[t]=e[t])}return r}var s=n.createContext({}),p=function(e){var a=n.useContext(s),t=a;return e&&(t="function"==typeof e?e(a):o(o({},a),e)),t},c=function(e){var a=p(e.components);return n.createElement(s.Provider,{value:a},e.children)},u="mdxType",m={inlineCode:"code",wrapper:function(e){var a=e.children;return n.createElement(n.Fragment,{},a)}},d=n.forwardRef((function(e,a){var t=e.components,r=e.mdxType,i=e.originalType,s=e.parentName,c=l(e,["components","mdxType","originalType","parentName"]),u=p(t),d=r,g=u["".concat(s,".").concat(d)]||u[d]||m[d]||i;return t?n.createElement(g,o(o({ref:a},c),{},{components:t})):n.createElement(g,o({ref:a},c))}));function g(e,a){var t=arguments,r=a&&a.mdxType;if("string"==typeof e||r){var i=t.length,o=new Array(i);o[0]=d;var l={};for(var s in a)hasOwnProperty.call(a,s)&&(l[s]=a[s]);l.originalType=e,l[u]="string"==typeof e?e:r,o[1]=l;for(var p=2;p<i;p++)o[p]=t[p];return n.createElement.apply(null,o)}return n.createElement.apply(null,t)}d.displayName="MDXCreateElement"},8702:(e,a,t)=>{t.r(a),t.d(a,{assets:()=>s,contentTitle:()=>o,default:()=>m,frontMatter:()=>i,metadata:()=>l,toc:()=>p});var n=t(8168),r=(t(6540),t(5680));const i={title:"Shipping your code to cloud with Scala, Besom and Pulumi",authors:{name:"\u0141ukasz Bia\u0142y",title:"Scala Developer Advocate, Besom team lead",url:"https://x.com/lukasz_bialy",image_url:"https://github.com/lbialy.png",email:"lbialy@virtuslab.com"}},o=void 0,l={permalink:"/besom/blog/2024/03/01/0.2.x-release",editUrl:"https://github.com/VirtusLab/besom/tree/main/website/blog/2024-03-01-0.2.x-release.md",source:"@site/blog/2024-03-01-0.2.x-release.md",title:"Shipping your code to cloud with Scala, Besom and Pulumi",description:"Besom, the Scala SDK for Pulumi, has just had it's second release - the 0.2.x series - that focuses on improving the developer experience based on the feedback from early adopters. This release is also the first release marking our march towards the stability of the APIs crafted with awesome developer experience in mind. Before getting into new features and changes let's step back a bit and discuss what Besom really is and why we've built it.",date:"2024-03-01T00:00:00.000Z",formattedDate:"March 1, 2024",tags:[],readingTime:13.935,hasTruncateMarker:!0,authors:[{name:"\u0141ukasz Bia\u0142y",title:"Scala Developer Advocate, Besom team lead",url:"https://x.com/lukasz_bialy",image_url:"https://github.com/lbialy.png",email:"lbialy@virtuslab.com",imageURL:"https://github.com/lbialy.png"}],frontMatter:{title:"Shipping your code to cloud with Scala, Besom and Pulumi",authors:{name:"\u0141ukasz Bia\u0142y",title:"Scala Developer Advocate, Besom team lead",url:"https://x.com/lukasz_bialy",image_url:"https://github.com/lbialy.png",email:"lbialy@virtuslab.com",imageURL:"https://github.com/lbialy.png"}}},s={authorsImageUrls:[void 0]},p=[{value:"A wider perspective",id:"a-wider-perspective",level:3},{value:"The App",id:"the-app",level:3},{value:"Run App, Run!",id:"run-app-run",level:3}],c={toc:p},u="wrapper";function m(e){let{components:a,...i}=e;return(0,r.yg)(u,(0,n.A)({},c,i,{components:a,mdxType:"MDXLayout"}),(0,r.yg)("p",null,"Besom, the Scala SDK for Pulumi, has just had it's second release - the ",(0,r.yg)("a",{parentName:"p",href:"https://virtuslab.github.io/besom/docs/changelog#020-08-02-2024"},"0.2.x")," series - that focuses on improving the developer experience based on the feedback from early adopters. This release is also the first release marking our march towards the stability of the APIs crafted with awesome developer experience in mind. Before getting into new features and changes let's step back a bit and discuss what Besom really is and why we've built it."),(0,r.yg)("h3",{id:"a-wider-perspective"},"A wider perspective"),(0,r.yg)("p",null,"Current landscape of software engineering is different in comparison to what it was just a decade ago when Terraform launched but even then the trend towards programmatic management of not only business problems but also software and hardware operations was clearly visible. The space itself was dominated by fragile bash scripting and configuration management DSLs that required additional learning and provided little elasticity. The complexity of software itself grew however as Internet grew ever bigger. It is therefore our opinion that software engineering will evolve to incorporate platform engineering as a first-class citizen - not as a thing that a remote operations teams does but as a natural part of application lifecycle that every developer can easily work with. This merge is already happening with new emergent technologies like ",(0,r.yg)("a",{parentName:"p",href:"https://klo.dev"},"Klotho")," and ",(0,r.yg)("a",{parentName:"p",href:"https://nitric.io/"},"Nitric")," but what is first necessary for proliferation of this approach is the common fundamental abstraction of platform resources. "),(0,r.yg)("p",null,"We believe that ",(0,r.yg)("a",{parentName:"p",href:"https://pulumi.com"},"Pulumi")," is the solution for Infrastructure as Software (IaS) - a tool like Terraform but much more elastic, much more accessible, composable and developer-friendly. Pulumi can be used from any language that offers a SDK and that's how it marries the ops with the dev - you use the tool that you use to declare resources your application will use and execute on. In the same vein of modern requirements in IT - scalability, robustness, reliability and performance - Scala is a proven contender that keeps evolving to meet new demands. We've felt that the next frontier the scalable language can manage is the domain of platform engineering. For this reason we have joined our forces with Pulumi to first bring Pulumi to JVM with the ",(0,r.yg)("a",{parentName:"p",href:"https://github.com/pulumi/pulumi-java"},"Java SDK")," and then, based on that experience, built ",(0,r.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom"},"Besom, the Scala SDK for Pulumi"),". Besom is therefore a way to declare and manage infrastructural resources in an idiomatic, functional way using Scala."),(0,r.yg)("p",null,"I am, personally, of the opinion that code is worth a million words so the best way to show you how easy reliable",(0,r.yg)("sup",{parentName:"p",id:"fnref-1-ced4a1"},(0,r.yg)("a",{parentName:"sup",href:"#fn-1-ced4a1",className:"footnote-ref"},"1"))," cloud engineering becomes with Besom is to just write some code! Quite often what you want to do is to just run your small app somewhere. Let's assume, for the sake of example, that \"somewhere\" is the Google Cloud Platform's Cloud Run service which is a managed runtime for containerised applications. "),(0,r.yg)("h3",{id:"the-app"},"The App"),(0,r.yg)("p",null,"Let's start with a small web app. To keep things as simple as possible we'll use ",(0,r.yg)("a",{parentName:"p",href:"https://scala-cli.virtuslab.org/"},"scala-cli")," which will take care of packaging our application for us. We'll also use ",(0,r.yg)("a",{parentName:"p",href:"https://tapir.softwaremill.com/en/latest/server/jdkhttp.html"},"tapir's")," JDK HTTP server support and leverage Loom because it's just free performance. Let's create a directory for the whole project somewhere and then put our app in ",(0,r.yg)("inlineCode",{parentName:"p"},"./app")," subdirectory. The ",(0,r.yg)("inlineCode",{parentName:"p"},"project.scala")," file will look like this: "),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'//> using scala 3.3.3\n//> using options -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement\n\n//> using jvm "graalvm-java21:21.0.2"\n\n//> using dep com.softwaremill.sttp.tapir::tapir-jdkhttp-server:1.9.10\n//> using dep com.softwaremill.sttp.tapir::tapir-files:1.9.10\n//> using dep com.outr::scribe:3.13.0\n\n//> using publish.organization org.virtuslab.besom.example\n//> using publish.name app\n\n//> using resourceDir resources\n')),(0,r.yg)("p",null,"The ",(0,r.yg)("inlineCode",{parentName:"p"},"scala-cli")," directives are pretty self-explanatory:"),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},"we use Scala 3.3.3 LTS"),(0,r.yg)("li",{parentName:"ul"},"we add some linting options for the compiler"),(0,r.yg)("li",{parentName:"ul"},"we select GraalVM CE for JDK 21 as the runtime (because of Loom and because GraalVM is awesome)"),(0,r.yg)("li",{parentName:"ul"},"we also pull some dependencies - tapir for http server along with static file serving module and scribe for logging"),(0,r.yg)("li",{parentName:"ul"},"we name the project for publishing"),(0,r.yg)("li",{parentName:"ul"},"and finally we designate a directory that will contain our resources")),(0,r.yg)("p",null,"The whole app fits in a single file - two endpoints, a HTML snippet as string and a http server definition:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'package org.virtuslab.besom.example\n\nimport sttp.tapir.*\nimport sttp.tapir.server.jdkhttp.*\nimport sttp.tapir.files.*\nimport java.util.concurrent.Executors\nimport sttp.model.HeaderNames\n\n@main def main(): Unit =\n  // required by Cloud Run Container runtime contract\n  // https://cloud.google.com/run/docs/reference/container-contract\n  val host = "0.0.0.0"\n  val port = sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(8080)\n\n  // handle index path only\n  val indexEndpoint = endpoint.get\n    .in("")\n    .in(extractFromRequest(_.connectionInfo.remote))\n    .in(header[Option[String]](HeaderNames.XForwardedFor))\n    .out(htmlBodyUtf8)\n    .handle { case (requestHost, xff) =>\n      val remote = xff.orElse(requestHost).getOrElse("unknown")\n      val forwarded = if xff.isDefined then "forwarded" else "not forwarded"\n\n      scribe.info(s"Received request from $remote ($forwarded) serving index.html...")\n\n      Right(index)\n    }\n\n  // serve resources in "static" directory under static/ path\n  val staticResourcesEndpoint =\n    staticResourcesGetServerEndpoint[Id]("static")(\n      // use classloader used to load this application\n      classOf[this.type].getClassLoader,\n      "static"\n    )\n\n  scribe.info(s"Starting server on $host:$port")\n  val _ = JdkHttpServer()\n    // use Loom\'s virtual threads to dispatch requests\n    .executor(Executors.newVirtualThreadPerTaskExecutor())\n    .host(host)\n    .port(port)\n    .addEndpoint(staticResourcesEndpoint)\n    .addEndpoint(indexEndpoint)\n    .start()\n\n// html template using tailwind css\nval index: String =\n  """\n  <!DOCTYPE html>\n  <html lang="en">\n  <head>\n    <meta charset="UTF-8">\n    <meta name="viewport" content="width=device-width, initial-scale=1.0">\n    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">\n    <title>Scala 3 app!</title>\n  </head>\n  <body>\n    <div class="flex h-screen w-full justify-center items-center">\n      <img src="static/scala.png" alt="scala logo"/>\n    </div>\n  </body>\n  </html>\n  """\n')),(0,r.yg)("p",null,"It doesn't do much as you can see as it only solves the hardest problem in web development using tailwind and displays Scala logo centered horizontally and vertically. "),(0,r.yg)("p",null,"We can check if tailwind did a good job by simply executing one command in the directory storing both files and navigating to ",(0,r.yg)("inlineCode",{parentName:"p"},"localhost:8080")," in browser."),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"scala-cli run app\n")),(0,r.yg)("p",null,"If you don't have ",(0,r.yg)("inlineCode",{parentName:"p"},"scala-cli")," on your machine already it is super ",(0,r.yg)("a",{parentName:"p",href:"https://scala-cli.virtuslab.org/install/"},"easy to fix"),", e.g.:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"curl -sSLf https://scala-cli.virtuslab.org/get | sh\n")),(0,r.yg)("h3",{id:"run-app-run"},"Run App, Run!"),(0,r.yg)("p",null,'We have our app and now we need to deploy it "somewhere" using Besom. To ',(0,r.yg)("a",{parentName:"p",href:"https://virtuslab.github.io/besom/docs/getting_started"},"get started")," we need to install Pulumi:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"curl -fsSL https://get.pulumi.com/ | sh\n")),(0,r.yg)("p",null,"and then install Scala language plugin for Pulumi:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi plugin install language scala 0.2.2 --server github://api.github.com/VirtusLab/besom\n")),(0,r.yg)("p",null,"Having done that we can initialise a new project:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"mkdir infra && cd infra\n")),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi new https://github.com/VirtusLab/besom/tree/v0.2.2/templates/gcp\n")),(0,r.yg)("p",null,"This will launch project builder, ask us some basic questions about how the project will be called, how the initial stack will be called and also what password should be used to encrypt the secrets",(0,r.yg)("sup",{parentName:"p",id:"fnref-2-ced4a1"},(0,r.yg)("a",{parentName:"sup",href:"#fn-2-ced4a1",className:"footnote-ref"},"2")),". We end up with 4 files inside of ",(0,r.yg)("inlineCode",{parentName:"p"},"./infra")," directory:"),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("inlineCode",{parentName:"li"},"Main.scala")," which contains our infrastructure as Scala code"),(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("inlineCode",{parentName:"li"},"project.scala")," which contains ",(0,r.yg)("inlineCode",{parentName:"li"},"scala-cli")," directives"),(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("inlineCode",{parentName:"li"},"Pulumi.yaml")," which contains public settings of the project"),(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("inlineCode",{parentName:"li"},"Pulumi.$STACK.yaml")," which contains the secrets bound to particular stack where ",(0,r.yg)("inlineCode",{parentName:"li"},"$STACK")," is the name we have provided in the previous step ")),(0,r.yg)("p",null,"To start we will need to add docker support for besom so that we can push our application's image to Google Container Registry (GCR). To do that we need to put these lines into ",(0,r.yg)("inlineCode",{parentName:"p"},"project.scala"),":"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'//> using dep "org.virtuslab::besom-docker:4.5.3-core.0.3"\n')),(0,r.yg)("p",null,"Since we want Besom to manage our image building we won't be able to benefit from ",(0,r.yg)("inlineCode",{parentName:"p"},"scala-cli"),"'s built-in Docker packaging capabilities but that's actually fine because we would like our Cloud Run app to have the smallest possible footprint possible. To do that we will need to leverage another application packaging capability of ",(0,r.yg)("inlineCode",{parentName:"p"},"scala-cli")," - that of GraalVM native-image building. Besom's Docker integration expects us to provide a ",(0,r.yg)("inlineCode",{parentName:"p"},"Dockerfile")," that will describe how to create the image and that's exactly what we're going to do by putting this content into ",(0,r.yg)("inlineCode",{parentName:"p"},"./app/Dockerfile"),": "),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-Dockerfile"},'FROM --platform=linux/amd64 debian:stable-slim AS build-env\n\nARG DEBIAN_FRONTEND=noninteractive\nRUN --mount=type=cache,target=/var/cache/apt \\\n    apt-get update  \\\n    && apt-get install -y curl gzip build-essential libz-dev zlib1g-dev\n\n# Install Scala CLI\nARG SCALA_CLI_VERSION=1.1.2\nRUN \\\n    curl -sSLf https://github.com/VirtusLab/scala-cli/releases/download/v${SCALA_CLI_VERSION}/scala-cli-x86_64-pc-linux.gz | gunzip -c > /usr/local/bin/scala-cli  \\\n    && chmod +x /usr/local/bin/scala-cli  \\\n    && /usr/local/bin/scala-cli version\n\nFROM --platform=linux/amd64 build-env AS build\nWORKDIR /src\n# Copy local code to the container image\nCOPY . .\n# Build a native binary with Scala CLI\nARG GRAALVM_VERSION=21.0.2\nRUN \\\n    --mount=type=cache,target=/root/.cache/coursier \\\n    /usr/local/bin/scala-cli --power package . \\\n    --suppress-experimental-feature-warning \\\n    --server=false \\\n    --jvm=graalvm-java21:${GRAALVM_VERSION} \\\n    --native-image \\\n    --graalvm-jvm-id=graalvm-java21:${GRAALVM_VERSION} \\\n    --graalvm-args="--static" \\\n    --graalvm-args="--install-exit-handlers" \\\n    --graalvm-args="--no-fallback" \\\n    --graalvm-args="-H:IncludeResources=.*png" \\\n    --main-class org.virtuslab.besom.example.main -o app -f\n\nFROM --platform=linux/amd64 gcr.io/distroless/static\n# Copy the binary to the production image from the builder stage\nCOPY --from=build /src/app /bin/app\n\n# Run the web service on container startup.\nCMD ["/bin/app"]\n')),(0,r.yg)("p",null,"This multi-stage ",(0,r.yg)("inlineCode",{parentName:"p"},"Dockerfile")," will yield a very small final (34.4 MB!) image that will contain only the binary of our app. It also takes care of cross-compilation if necessary."),(0,r.yg)("p",null,"With this set up we can finally write down infrastructure necessary to deploy our service. Let's clear out our main function's body and start with some basic definitions:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'@main def main = Pulumi.run {\n  val project = config.requireString("gcp:project")\n  val region  = config.requireString("gcp:region")\n  \n  val repoName      = p"gcr.io/${project}/${pulumiProject}" // will be automatically created by GCP on docker push\n  val appName       = "app"\n  val imageFullName = p"${repoName}/${appName}:latest"\n  \n  Stack.exports(\n    dockerImage = imageFullName\n  )\n')),(0,r.yg)("p",null,"First we declare that our program needs some values from configuration regarding the GCP project ID and the region to which we want to deploy to. Configuration will be returned as ",(0,r.yg)("inlineCode",{parentName:"p"},"Output[String]")," values which are roughly equal to ",(0,r.yg)("inlineCode",{parentName:"p"},"IO[String]")," (or ",(0,r.yg)("inlineCode",{parentName:"p"},"Task[String]")," if you prefer ZIO)  but we need to interpolate that information to build the image name for GCR. Usually that would involve writing something akin to:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'val imageFullName = project.map { proj =>\n  val repoName = s"gcr.io/${proj}/${pulumiProject}"\n  val appName = "app"\n  s"${repoName}/${appName}:latest"\n}    \n')),(0,r.yg)("p",null,"but Besom offers a ",(0,r.yg)("inlineCode",{parentName:"p"},'p""')," / ",(0,r.yg)("inlineCode",{parentName:"p"},'pulumi""')," string interpolator that simplifies this a lot and returns ",(0,r.yg)("inlineCode",{parentName:"p"},"Output[String]")," for us."),(0,r.yg)("p",null,"Google Cloud Platform requires us to enable services that we wish to use and therefore the first Besom resource that we have to create will be ",(0,r.yg)("inlineCode",{parentName:"p"},"gcp.projects.Service")," that allows us to manage service configuration for the project. We will future-proof it a bit with a small enum and make a small, helpful wrapper for the capabilities that we need:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'  enum GoogleApis(val name: String):\n    case CloudRun extends GoogleApis("run.googleapis.com")\n    def enableApiKey: NonEmptyString = s"enable-${name.replace(".", "-")}"\n\n  // Enable GCP service(s) for the current project\n  val enableServices: Map[GoogleApis, Output[gcp.projects.Service]] = List(\n    GoogleApis.CloudRun\n  ).map(api =>\n    api -> gcp.projects.Service(\n      api.enableApiKey,\n      gcp.projects.ServiceArgs(\n        project = project,\n        service = api.name,\n        /* if true - at every destroy this will disable the dependent services for the whole project */\n        disableDependentServices = true,\n        /* if true - at every destroy this will disable the service for the whole project */\n        disableOnDestroy = true\n      )\n    )\n  ).toMap\n')),(0,r.yg)("p",null,"For these services to be configured (there's just one for now) we have to make the Stack aware of the necessity their evaluation. We modify the result of our main function then:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"Stack(\n  Output.sequence(enableServices.values)\n).exports(\n  dockerImage = imageFullName\n)\n")),(0,r.yg)("p",null,"With this change we can run our Besom project using ",(0,r.yg)("inlineCode",{parentName:"p"},"pulumi up")," in ",(0,r.yg)("inlineCode",{parentName:"p"},"./infra")," directory and Pulumi will be able to enable Cloud Run service for us."),(0,r.yg)("p",null,"Next step is for Besom to build and push our Docker image for us. This bit is quite straightforwards - we only need to introduce a new import on the top of the file:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"import besom.api.docker\n")),(0,r.yg)("p",null,"and then declare a ",(0,r.yg)("inlineCode",{parentName:"p"},"docker.Image")," resource like this:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'  // Build a Docker image from our Scala app and push it to GCR\n  val image = docker.Image(\n    "image",\n    docker.ImageArgs(\n      imageName = imageFullName,\n      build = docker.inputs.DockerBuildArgs(\n        context = p"../${appName}",\n        platform = "linux/amd64" // Cloud Run only supports linux/amd64\n      )\n    )\n  )\n')),(0,r.yg)("p",null,"We provide a full name of the image that we've constructed before, the path to Docker context - our ",(0,r.yg)("inlineCode",{parentName:"p"},"Dockerfile")," in ",(0,r.yg)("inlineCode",{parentName:"p"},"./app")," folder and the platform for which we want the image to be built for. "),(0,r.yg)("p",null,"Next step is to deploy a Cloud Run service:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'  val service = gcp.cloudrun.Service(\n    "service",\n    gcp.cloudrun.ServiceArgs(\n      location = region,\n      name = appName,\n      template = gcp.cloudrun.inputs.ServiceTemplateArgs(\n        spec = gcp.cloudrun.inputs.ServiceTemplateSpecArgs(\n          containers = gcp.cloudrun.inputs.ServiceTemplateSpecContainerArgs(\n            image = image.imageName,\n            resources = gcp.cloudrun.inputs.ServiceTemplateSpecContainerResourcesArgs(\n              limits = Map(\n                "memory" -> "1Gi"\n              )\n            )\n          ) :: Nil\n        )\n      )\n    ),\n    opts(dependsOn = enableServices(GoogleApis.CloudRun))\n  )\n')),(0,r.yg)("p",null,"This is a bit more involved as there are more properties to configure:"),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},"we pass information about the region in which this deployment should happen in"),(0,r.yg)("li",{parentName:"ul"},"we set up the name of the Cloud Run service - ",(0,r.yg)("inlineCode",{parentName:"li"},"app")),(0,r.yg)("li",{parentName:"ul"},"we then specify that we want to use our container with 1 GB of RAM")),(0,r.yg)("p",null,"One important thing here is that we pass a resource option declaring that this resource has to be created only after Cloud Run service is enabled. Pulumi is parallel by default not to choke on relatively slow cloud provider APIs and therefore some sequential relationships between resources require a manual setup of the dependency in the evaluation graph. This is not necessary when an output value of one resource is used as an input for another but in this case we have to give Pulumi a small hint."),(0,r.yg)("p",null,"Finally, we have to inform GCP that we want anyone to be able to reach our application from the internet by configuring an IAM member resource:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'  // Open the service to public unrestricted access\n  val serviceIam = gcp.cloudrun.IamMember(\n    "service-iam-everyone",\n    gcp.cloudrun.IamMemberArgs(\n      location = service.location,\n      service = service.name,\n      role = "roles/run.invoker",\n      member = "allUsers"\n    )\n  )\n')),(0,r.yg)("p",null,"The last thing to do is to add the URL of our app to Stack's exports and add the IAM member to dependencies of our Stack:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"Stack(\n  Output.sequence(enableServices.values),\n  serviceIam\n).exports(\n  dockerImage = imageFullName,\n  // Export the DNS name of the service\n  serviceUrl = service.statuses.map(_.headOption.map(_.url)) \n)\n")),(0,r.yg)("p",null,"After this step we can run:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi up\n")),(0,r.yg)("p",null,"in our ",(0,r.yg)("inlineCode",{parentName:"p"},"./infra")," directory and let Pulumi handle all the heavy lifting. After a while we will see an output:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre"},'Please choose a stack, or create a new one: dev\nPreviewing update (dev):\n     Type                       Name                       Plan\n +   pulumi:pulumi:Stack        gcp-cloudrun-dev           create\n +   \u251c\u2500 gcp:projects:Service    enable-run-googleapis-com  create\n +   \u251c\u2500 docker:index:Image      image                      create\n +   \u251c\u2500 gcp:cloudrun:Service    service                    create\n +   \u2514\u2500 gcp:cloudrun:IamMember  service-iam-everyone       create\n\nOutputs:\n    dockerImage: "gcr.io/redacted-redacted/gcp-cloudrun/app:latest"\n    serviceUrl : output<string>\n\nResources:\n    + 5 to create\n\nDo you want to perform this update? yes\nUpdating (dev):\n     Type                       Name                       Status\n +   pulumi:pulumi:Stack        gcp-cloudrun-dev           created (276s)\n +   \u251c\u2500 gcp:projects:Service    enable-run-googleapis-com  created (4s)\n +   \u251c\u2500 docker:index:Image      image                      created (166s)\n +   \u251c\u2500 gcp:cloudrun:Service    service                    created (109s)\n +   \u2514\u2500 gcp:cloudrun:IamMember  service-iam-everyone       created (4s)\n\nOutputs:\n    dockerImage: "gcr.io/redacted-redacted/gcp-cloudrun/app:latest"\n    serviceUrl : "https://app-kdthccbreq-lm.a.run.app"\n\nResources:\n    + 5 created\n\nDuration: 4m44s\n')),(0,r.yg)("p",null,"We can now copy the output of ",(0,r.yg)("inlineCode",{parentName:"p"},"serviceUrl")," property to the browser and enjoy our new app being live!"),(0,r.yg)("p",null,"Cloud Run offers some nice observability features from the start so let's generate some traffic to see that everything is about right:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"wrk -t5 -c25 -d120s https://app-kdthccbreq-lm.a.run.app\n")),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre"},"Running 2m test @ https://app-kdthccbreq-lm.a.run.app\n  5 threads and 25 connections\n  Thread Stats   Avg      Stdev     Max   +/- Stdev\n    Latency    17.68ms   10.49ms 455.92ms   98.89%\n    Req/Sec   284.36     38.99   350.00     92.98%\n  165617 requests in 2.00m, 112.77MB read\n  Socket errors: connect 0, read 0, write 0, timeout 24\nRequests/sec:   1379.24\nTransfer/sec:      0.94MB\n")),(0,r.yg)("p",null,"And that's how it looks in the GCP Console:"),(0,r.yg)("p",null,(0,r.yg)("img",{alt:"metrics",src:t(6207).A,width:"3354",height:"1506"})),(0,r.yg)("p",null,"To clean up we can simply run:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-bash"},"pulumi destroy\n")),(0,r.yg)("p",null,"and all resources registered with the stack will be removed: "),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre"},'Please choose a stack: dev\nPreviewing destroy (dev):\n     Type                       Name                       Plan\n -   pulumi:pulumi:Stack        gcp-cloudrun-dev           delete\n -   \u251c\u2500 gcp:cloudrun:IamMember  service-iam-everyone       delete\n -   \u251c\u2500 gcp:cloudrun:Service    service                    delete\n -   \u251c\u2500 gcp:projects:Service    enable-run-googleapis-com  delete\n -   \u2514\u2500 docker:index:Image      image                      delete\n\nOutputs:\n  - dockerImage: "gcr.io/besom-413811/gcp-cloudrun/app:latest"\n  - serviceUrl : "https://app-kdthccbreq-lm.a.run.app"\n\nResources:\n    - 5 to delete\n\nDo you want to perform this update? yes\nDestroying (dev):\n     Type                       Name                       Status\n -   pulumi:pulumi:Stack        gcp-cloudrun-dev           deleted (0.00s)\n -   \u251c\u2500 gcp:cloudrun:IamMember  service-iam-everyone       deleted (5s)\n -   \u251c\u2500 gcp:cloudrun:Service    service                    deleted (0.13s)\n -   \u251c\u2500 gcp:projects:Service    enable-run-googleapis-com  deleted (12s)\n -   \u2514\u2500 docker:index:Image      image                      deleted (0.00s)\n\nOutputs:\n  - dockerImage: "gcr.io/besom-413811/gcp-cloudrun/app:latest"\n  - serviceUrl : "https://app-kdthccbreq-lm.a.run.app"\n\nResources:\n    - 5 deleted\n\nDuration: 19s\n\nThe resources in the stack have been deleted, but the history and configuration associated with the stack are still maintained.\nIf you want to remove the stack completely, run `pulumi stack rm dev`.\n')),(0,r.yg)("p",null,"Fun part about this is that it's just as easy to set things up using any other cloud provider, even the smaller and less popular ones. Besom's provider library includes all (since 0.2.0) publicly available ",(0,r.yg)("a",{parentName:"p",href:"https://www.pulumi.com/registry/"},"Pulumi providers")," and ",(0,r.yg)("a",{parentName:"p",href:"https://central.sonatype.com/search?q=besom&sort=published"},"counts around 150 integrations")," at the moment. "),(0,r.yg)("p",null,"The full code for this app can be found ",(0,r.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom-scala-on-cloudrun"},"here"),"."),(0,r.yg)("p",null,"This sample application has been adapted from our library of examples available ",(0,r.yg)("a",{parentName:"p",href:"https://github.com/VirtusLab/besom/tree/release/v0.2.2/examples"},"here")," - we invite you to check out others as well. While waiting anxiously to see what you build with Besom we'll keep ourselves busy improving the developer experience even further and delivering the ",(0,r.yg)("a",{parentName:"p",href:"https://www.pulumi.com/automation/"},"Automation API"),". "),(0,r.yg)("div",{className:"footnotes"},(0,r.yg)("hr",{parentName:"div"}),(0,r.yg)("ol",{parentName:"div"},(0,r.yg)("li",{parentName:"ol",id:"fn-1-ced4a1"},"I know you can just set things up in the console of your cloud provider but common experience proves that this approach works for about a week or month and then the decay sets in and after a while someone deletes prod by accident",(0,r.yg)("a",{parentName:"li",href:"#fnref-1-ced4a1",className:"footnote-backref"},"\u21a9")),(0,r.yg)("li",{parentName:"ol",id:"fn-2-ced4a1"},(0,r.yg)("strong",{parentName:"li"},"important"),", always provide a good password here, it's used to encrypt secrets so that you can store them in your git repo",(0,r.yg)("a",{parentName:"li",href:"#fnref-2-ced4a1",className:"footnote-backref"},"\u21a9")))))}m.isMDXComponent=!0},6207:(e,a,t)=>{t.d(a,{A:()=>n});const n=t.p+"assets/images/metrics-9c9a8c6dc492080260d463952b8d7730.png"}}]);