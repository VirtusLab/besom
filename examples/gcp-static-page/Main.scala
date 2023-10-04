import besom.*
import besom.api.gcp
import besom.api.gcp.storage.inputs.BucketWebsiteArgs
import besom.api.gcp.storage.{BucketAccessControlArgs, BucketArgs, BucketObjectArgs}

@main def main = Pulumi.run {
  // Create new storage bucket in the US multi-region
  // and settings for main_page_suffix and not_found_page
  val staticWebsite = gcp.storage.Bucket(
    "static-website-bucket",
    BucketArgs(
      location = "US",
      storageClass = "STANDARD",
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
      bucket = staticWebsite.id
    )
  )

  // Upload a simple 404 / error page to the bucket
  val errorPage = gcp.storage.BucketObject(
    "404.html",
    BucketObjectArgs(
      name = "404.html",
      content = "<html><body><h1>404 - Not Found</h1></body></html>",
      contentType = "text/html",
      bucket = staticWebsite.id
    )
  )

  // Make bucket public by granting allUsers READER access
  val publicRule = gcp.storage.BucketAccessControl(
    "allUsers",
    BucketAccessControlArgs(
      bucket = staticWebsite.id,
      role = "READER",
      entity = "allUsers"
    )
  )

  for
    staticWebsite <- staticWebsite
    _             <- indexPage
    _             <- errorPage
    _             <- publicRule
  yield exports(
    bucketName = staticWebsite.name,
    bucketUrl = staticWebsite.url
  )
}
