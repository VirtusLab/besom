import besom.*
import besom.api.aws.*

@main def main = Pulumi.run {
  for
    bucket <- s3.bucket("my-bucket")
  yield Pulumi.exports(
    bucketName = bucket.bucket
  )
}