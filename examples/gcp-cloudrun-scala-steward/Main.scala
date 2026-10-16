import besom.*
import besom.api.gcp
import besom.api.gcp.cloudrunv2.inputs.*
import besom.api.gcp.cloudrunv2.{Job, JobArgs, JobIamMember, JobIamMemberArgs}
import besom.api.gcp.cloudscheduler as csh
import besom.api.gcp.cloudscheduler.inputs.*
import besom.api.gcp.secretmanager.*
import besom.api.gcp.secretmanager.inputs.*
import besom.api.gcp.serviceaccount.{Account, AccountArgs}
import besom.api.gcp.storage.{Bucket, BucketArgs, BucketObject, BucketObjectArgs, BucketIamMember, BucketIamMemberArgs}

@main def main = Pulumi.run {
  val repoFileName        = "repos.md"
  val volumePath          = "/opt/scala-steward"
  val appName             = "scala-steward"
  val scalaStewardVersion = "latest"
  val gitConfig           = config.requireObject[GitConfig]("git")
  val gcpConfig           = GcpConfig.apply

  val serviceAccount = Account(
    s"$appName-sa",
    AccountArgs(
      accountId = s"$appName-sa",
      displayName = "Service Account for Scala Steward"
    )
  )
  val serviceAccountMember = p"serviceAccount:${serviceAccount.email}"

  // Create a Cloud Storage bucket
  val bucket = Bucket(
    s"$appName-bucket",
    BucketArgs(
      location = "US",
      forceDestroy = true
    )
  )

  // Grant the Cloud Run job service account permissions to access and delete objects in the bucket
  val bucketIamMember = BucketIamMember(
    s"$appName-bucket-access",
    BucketIamMemberArgs(
      bucket = bucket.name,
      role = "roles/storage.objectAdmin", // This role includes permissions to delete objects
      member = serviceAccountMember
    )
  )

  val reposObject = BucketObject(
    s"$appName-repos",
    BucketObjectArgs(
      bucket = bucket.name,
      name = repoFileName,
      source = besom.Asset.FileAsset(s"./$repoFileName")
    )
  )

  val secret = Secret(
    s"$appName-secret",
    SecretArgs(
      secretId = s"$appName-git-secret",
      replication = SecretReplicationArgs(
        auto = SecretReplicationAutoArgs()
      )
    ),
    opts(dependsOn = GCPService.SecretManager())
  )

  val secretVersion = SecretVersion(
    s"$appName-secret-version",
    SecretVersionArgs(
      secret = secret.name,
      secretData = gitConfig.password
    )
  )

  val secretIamMember = SecretIamMember(
    s"$appName-secret-access",
    SecretIamMemberArgs(
      secretId = secret.id,
      role = "roles/secretmanager.secretAccessor",
      member = serviceAccountMember
    ),
    opts(dependsOn = secret)
  )

  val scalaStewardRun =
    List(
      p"/opt/docker/bin/scala-steward",
      p"--workspace $volumePath/workspace",
      p"--repos-file $volumePath/${reposObject.name}",
      p"--git-author-email ${gitConfig.gitAuthorEmail}",
      p"--forge-type ${gitConfig.forgeType}",
      p"--forge-api-host ${gitConfig.forgeApiHost}",
      p"--forge-login ${gitConfig.forgeLogin}",
      p"--git-ask-pass ${GitAskPassFile.gitPassPath}",
      p"--do-not-fork"
    ).sequence.map(_.mkString(" "))

  // Define the Cloud Run Job
  val cloudRunJob = Job(
    s"$appName-job",
    JobArgs(
      location = gcpConfig.region,
      template = JobTemplateArgs(
        template = JobTemplateTemplateArgs(
          serviceAccount = serviceAccount.email,
          timeout = "2400s", // 40 min
          maxRetries = 2,
          containers = JobTemplateTemplateContainerArgs(
            // Scala Steward Docker image
            image = p"fthomas/scala-steward:$scalaStewardVersion",
            envs = JobTemplateTemplateContainerEnvArgs(
              name = GitAskPassFile.gitPassEnvName,
              valueSource = JobTemplateTemplateContainerEnvValueSourceArgs(
                secretKeyRef = JobTemplateTemplateContainerEnvValueSourceSecretKeyRefArgs(
                  secret = secret.id,
                  version = secretVersion.version
                )
              )
            ) :: Nil,
            commands = List(
              "/bin/sh",
              "-c",
              p"${GitAskPassFile.gitPassFile} && $scalaStewardRun"
            ),
            resources = JobTemplateTemplateContainerResourcesArgs(
              limits = Map("cpu" -> "2", "memory" -> "8Gi")
            ),
            volumeMounts = JobTemplateTemplateContainerVolumeMountArgs(
              name = "gcs-volume",
              mountPath = volumePath
            ) :: Nil
          ) :: Nil,
          volumes = JobTemplateTemplateVolumeArgs(
            name = "gcs-volume",
            gcs = JobTemplateTemplateVolumeGcsArgs(
              bucket = bucket.name,
              readOnly = false
            )
          ) :: Nil
        )
      )
    ),
    opts(dependsOn = GCPService.CloudRun())
  )

  // Grant the Cloud Scheduler service account permission to invoke the Cloud Run job
  val jobIamMember = JobIamMember(
    s"$appName-scheduler-invoker",
    JobIamMemberArgs(
      name = cloudRunJob.name,
      role = "roles/run.invoker",
      member = serviceAccountMember
    )
  )

  val schedulerUri =
    p"https://${gcpConfig.region}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${gcpConfig.project}/jobs/${cloudRunJob.name}:run"
  // Create a Cloud Scheduler job to trigger the Cloud Run job
  val schedulerJob = csh.Job(
    s"$appName-scheduler",
    csh.JobArgs(
      schedule = "0 12 * * *",
      timeZone = "Etc/UTC",
      httpTarget = JobHttpTargetArgs(
        httpMethod = "POST",
        uri = schedulerUri,
        oauthToken = JobHttpTargetOauthTokenArgs(
          serviceAccountEmail = serviceAccount.email
        )
      )
    ),
    opts(dependsOn = GCPService.Scheduler())
  )

  Stack(bucketIamMember, secretIamMember, jobIamMember)
    .exports(
      jobName = cloudRunJob.name,
      gitPasswordFile = GitAskPassFile.gitPassFile,
      scalaStewardRunScript = scalaStewardRun,
      schedulerJobName = schedulerJob.name,
      secretName = secret.name,
      secretVersionName = secretVersion.name
    )
}
