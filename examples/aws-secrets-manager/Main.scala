import besom.*
import besom.api.aws
import besom.api.aws.secretsmanager.SecretVersionArgs

@main def main = Pulumi.run {
  // Get the Pulumi secret value
  val mySecret = config.getSecret("aws-secrets-manager:mySecret")

  // Create an AWS secret
  val secret = aws.secretsmanager.Secret("mySecret")

  // Store a new secret version
  val secretVersion = aws.secretsmanager.SecretVersion(
    "secretVersion",
    SecretVersionArgs(
      secretId = secret.id,
      secretString = mySecret
    )
  )

  for
    secret <- secret
    _      <- secretVersion
  yield Pulumi.exports(
    secretId = secret.id // Export secret ID (in this case the ARN)
  )
}
