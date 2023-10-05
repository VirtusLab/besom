import besom.*
import besom.api.gcp
import besom.api.gcp.storage.BucketArgs

@main def main = Pulumi.run {
  for
    bucket <- gcp.storage.Bucket("my-bucket", BucketArgs(location = "US"))
  yield exports(
    bucketName = bucket.url // Export the DNS name of the bucket
  )
}