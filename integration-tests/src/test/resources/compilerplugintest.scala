//> using scala 3.3.1
//> using plugin "org.virtuslab::besom-compiler-plugin:0.0.1-SNAPSHOT"
//> using lib "org.virtuslab::besom-aws:0.0.1-SNAPSHOT"
//> using lib "org.virtuslab::besom-core:0.0.1-SNAPSHOT"

import besom.*
import besom.api.aws

@main
def main = Pulumi.run {
  val s3Bucket: Output[aws.s3.Bucket] = aws.s3.bucket("my-bucket")

  val s3BucketName = s"my-bucket-name-${s3Bucket.id}"

  for
    _ <- s3Bucket
  yield
    Pulumi.exports(
      s3BucketName = s3BucketName
    )
}
