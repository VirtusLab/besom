import besom.*
import besom.api.gcp
import besom.api.gcp.compute.*
import besom.api.gcp.compute.inputs.*
import besom.api.gcp.storage.*
import besom.api.gcp.storage.inputs.*

@main def main = Pulumi.run {
  // Create new storage bucket in the US multi-region
  // and settings for main_page_suffix and not_found_page
  val staticWebsite = gcp.storage.Bucket(
    "static-website-bucket",
    BucketArgs(
      location = "US",
      storageClass = "STANDARD",
      uniformBucketLevelAccess = true,
      forceDestroy = true, // delete bucket and contents on destroy
      cors = List(
        BucketCorArgs(
          origins = List("*"),
          methods = List("GET"),
          responseHeaders = List("*")
        )
      ),
      website = BucketWebsiteArgs(
        mainPageSuffix = "index.html",
        notFoundPage = "404.html"
      )
    )
  )

  // Upload a simple index.html page to the bucket
  val indexPage = gcp.storage.BucketObject(
    "index.html",
    BucketObjectArgs(
      name = "index.html",
      content = "<html><body><h1>Hello World!</h1></body></html>",
      contentType = "text/html",
      bucket = staticWebsite.name
    )
  )

  // Upload a simple 404 error page to the bucket
  val errorPage = gcp.storage.BucketObject(
    "404.html",
    BucketObjectArgs(
      name = "404.html",
      content = "<html><body><h1>404 - Not Found</h1></body></html>",
      contentType = "text/html",
      bucket = staticWebsite.name
    )
  )

  // Make bucket public by granting all users read access
  val publicRule = gcp.storage.BucketIAMMember(
    "allUsers",
    BucketIAMMemberArgs(
      bucket = staticWebsite.name,
      role = "roles/storage.objectViewer",
      member = "allUsers"
    )
  )

  // Create HTTP(s) proxy-based Layer 7 external Application Load Balancer resources

  // Reserve a public IP address that your audience uses to reach your load balancer
  val ip = gcp.compute.GlobalAddress("website-ip")

  // Setup load balancer backend that understands buckets
  val backendBucket = gcp.compute.BackendBucket(
    "website-backend",
    BackendBucketArgs(
      bucketName = staticWebsite.name,
      enableCdn = true
    )
  )

  // Define requests routing rules to map host and path of an incoming URL to a load balancer backend
  val paths = gcp.compute.URLMap(
    "website-urlmap",
    URLMapArgs(
      defaultService = backendBucket.id,
      hostRules = List(
        URLMapHostRuleArgs(
          hosts = List("*"),
          pathMatcher = "allpaths"
        )
      ),
      pathMatchers = List(
        URLMapPathMatcherArgs(
          name = "allpaths",
          defaultService = backendBucket.id,
          pathRules = List(
            URLMapPathMatcherPathRuleArgs(
              paths = List("/*"),
              service = backendBucket.id
            )
          )
        )
      )
    )
  )

  // Create HTTP target proxy to replace incoming connections from clients with connections from the load balancer
  val proxy = gcp.compute.TargetHttpProxy(
    "website-proxy",
    TargetHttpProxyArgs(
      urlMap = paths.id
    )
  )

  // Finally create a global forwarding rule to map the IP & port our target proxy
  val forwardingRule = gcp.compute.ForwardingRule(
    "website-http-forwarding-rule",
    ForwardingRuleArgs(
      ipProtocol = "TCP",
      portRange = "80",
      loadBalancingScheme = "EXTERNAL_MANAGED", // will use envoy-based Application Load Balancer
      target = proxy.id,
      ipAddress = ip.address,
    )
  )

  for
    staticWebsite <- staticWebsite
    _             <- indexPage
    _             <- errorPage
    _             <- publicRule
    _             <- ip
    _             <- backendBucket
    _             <- paths
    _             <- proxy
    _             <- forwardingRule
  yield exports(
    bucketName = staticWebsite.name,
    bucketUrl = staticWebsite.url,
    websiteIp = ip.address,
  )
}
