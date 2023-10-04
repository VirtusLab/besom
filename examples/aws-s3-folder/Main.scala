import besom.*
import besom.api.aws.s3
import besom.api.aws.s3.inputs.BucketWebsiteArgs
import besom.types.Asset.FileAsset
import besom.types.Output
import spray.json.*

import java.io.File
import java.nio.file.Files

val siteDir = "www"

@main def main = Pulumi.run {
  // Create a bucket and expose a website index document
  val siteBucket = s3.Bucket(
    "s3-website-bucket",
    s3.BucketArgs(
      website = BucketWebsiteArgs(
        indexDocument = "index.html"
      )
    )
  )

  val siteBucketName = siteBucket.bucket

  val siteBucketPublicAccessBlock = siteBucketName.flatMap { name =>
    s3.BucketPublicAccessBlock(
      s"${name}-publicaccessblock",
      s3.BucketPublicAccessBlockArgs(
        bucket = siteBucket.id,
        blockPublicPolicy = false // Do not block public bucket policies for this bucket
      )
    )
  }

  // Set the access policy for the bucket so all objects are readable
  val siteBucketPolicy = siteBucketName.flatMap(name =>
    s3.BucketPolicy(
      s"${name}-access-policy",
      s3.BucketPolicyArgs(
        bucket = siteBucket.id,
        policy = JsObject(
          "Version" -> JsString("2012-10-17"),
          "Statement" -> JsArray(
            JsObject(
              "Sid" -> JsString("PublicReadGetObject"),
              "Effect" -> JsString("Allow"),
              "Principal" -> JsObject(
                "AWS" -> JsString("*")
              ),
              "Action" -> JsArray(JsString("s3:GetObject")),
              "Resource" -> JsArray(JsString(s"arn:aws:s3:::${name}/*"))
            )
          )
        ).prettyPrint
      ),
      CustomResourceOptions(
        dependsOn = siteBucketPublicAccessBlock.map(List(_))
      )
    )
  )

  // For each file in the directory, create an S3 object stored in `siteBucket`
  val uploads: Output[List[s3.BucketObject]] = File(siteDir).listFiles().map { file =>
    val name = NonEmptyString(file.getName) match
      case Some(name) => name
      case None => throw new RuntimeException("Unexpected empty file name")
    s3.BucketObject(
      name,
      s3.BucketObjectArgs(
        bucket = siteBucket.id, // reference the s3.Bucket object
        source = FileAsset(file.getAbsolutePath), // use FileAsset to point to a file
        contentType = Files.probeContentType(file.toPath) // set the MIME type of the file
      ),
      CustomResourceOptions(
        dependsOn = siteBucket.map(List(_))
      )
    )
  }.toList.sequence

  for
    bucket <- siteBucket
    _      <- siteBucketPublicAccessBlock
    _      <- siteBucketPolicy
    _      <- uploads
  yield exports(
    bucketName = bucket.bucket,
    websiteUrl = bucket.websiteEndpoint
  )
}
