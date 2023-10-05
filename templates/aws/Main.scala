import besom.*
import besom.api.aws

@main def main = Pulumi.run {
  for
    bucket <- aws.s3.Bucket("my-bucket")
  yield exports(
    bucketName = bucket.bucket
  )
}