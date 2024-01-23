import besom.*
import besom.api.gcp
import besom.api.gcp.storage.BucketArgs

@main def main = Pulumi.run {
  val bucket = gcp.storage.Bucket("my-bucket", BucketArgs(location = "US"))

  Stack.exports(
    bucketName = bucket.url // Export the DNS name of the bucket
  )
}
