import besom.*
import besom.api.aws

@main def main = Pulumi.run {
  val bucket = aws.s3.Bucket("my-bucket")

  Stack.exports(
    bucketName = bucket.bucket
  )
}
