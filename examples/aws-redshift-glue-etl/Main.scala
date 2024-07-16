import besom.*
import besom.api.aws
import besom.json.*

@main def main = Pulumi.run {

  val clusterIdentifier = "my-redshift-cluster"
  val clusterDBName     = "dev"
  val clusterDBUsername = "admin"
  val clusterDBPassword = "Password!123"
  val glueDBName        = "my-glue-db"

  // Create an S3 bucket to store some raw data.
  val eventsBucket = aws.s3.Bucket(
    name = "events",
    aws.s3.BucketArgs(forceDestroy = true)
  )

  // Create a VPC.
  val vpc = aws.ec2.Vpc(
    name = "vpc",
    aws.ec2.VpcArgs(
      cidrBlock = "10.0.0.0/16",
      enableDnsHostnames = true
    )
  )

  // Create a private subnet within the VPC.
  val subnet = aws.ec2.Subnet(
    name = "subnet",
    aws.ec2.SubnetArgs(
      vpcId = vpc.id,
      cidrBlock = "10.0.1.0/24"
    )
  )

  // Declare a Redshift subnet group with the subnet ID.
  val subnetGroup = aws.redshift.SubnetGroup(
    name = "subnet-group",
    aws.redshift.SubnetGroupArgs(
      subnetIds = List(subnet.id)
    )
  )

  // Create an IAM role granting Redshift read-only access to S3.
  val redshiftRole = aws.iam.Role(
    name = "redshift-role",
    aws.iam.RoleArgs(
      assumeRolePolicy = json"""{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Action": "sts:AssumeRole",
                "Effect": "Allow",
                "Principal": {
                    "Service": "redshift.amazonaws.com"
                }
            }
        ]
    }""".map(_.prettyPrint),
      managedPolicyArns = List(
        aws.iam.enums.ManagedPolicy.AmazonS3ReadOnlyAccess.value
      )
    )
  )

  // Create a VPC endpoint so the cluster can read from S3 over the private network.
  val vpcEndpoint = aws.ec2.VpcEndpoint(
    name = "s3-vpc-endpoint",
    aws.ec2.VpcEndpointArgs(
      vpcId = vpc.id,
      serviceName = p"com.amazonaws.${aws.getRegion(aws.GetRegionArgs()).name}.s3",
      routeTableIds = List(vpc.mainRouteTableId)
    )
  )

  // Create a single-node Redshift cluster in the VPC.
  val cluster = aws.redshift.Cluster(
    name = "cluster",
    aws.redshift.ClusterArgs(
      clusterIdentifier = clusterIdentifier,
      databaseName = clusterDBName,
      masterUsername = clusterDBUsername,
      masterPassword = clusterDBPassword,
      nodeType = "ra3.xlplus",
      clusterSubnetGroupName = subnetGroup.name,
      clusterType = "single-node",
      publiclyAccessible = false,
      skipFinalSnapshot = true,
      vpcSecurityGroupIds = List(vpc.defaultSecurityGroupId),
      iamRoles = List(redshiftRole.arn)
    )
  )

  // Define an AWS cron expression of "every 15 minutes".
  // https://docs.aws.amazon.com/lambda/latest/dg/services-cloudwatchevents-expressions.html
  val every15minutes = "cron(0/15 * * * ? *)"

  // Create a Glue catalog database.
  val glueCatalogDB = aws.glue.CatalogDatabase(
    name = "glue-catalog-db",
    aws.glue.CatalogDatabaseArgs(
      name = glueDBName
    )
  )

// Define an IAM role granting AWS Glue access to S3 and other Glue-required services.
  val glueRole = aws.iam.Role(
    name = "glue-role",
    aws.iam.RoleArgs(
      assumeRolePolicy = json"""{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Action": "sts:AssumeRole",
                "Effect": "Allow",
                "Principal": {
                    "Service": "glue.amazonaws.com"
                }
            }
        ]
    }""".map(_.prettyPrint),
      managedPolicyArns = List(
        aws.iam.enums.ManagedPolicy.AmazonS3FullAccess.value,
        aws.iam.enums.ManagedPolicy.AWSGlueServiceRole.value
      )
    )
  )

  // Create a Glue crawler to process the contents of the data bucket on a schedule.
  // https://docs.aws.amazon.com/glue/latest/dg/monitor-data-warehouse-schedule.html
  val glueCrawler = aws.glue.Crawler(
    name = "glue-crawler",
    aws.glue.CrawlerArgs(
      databaseName = glueCatalogDB.name,
      role = glueRole.arn,
      schedule = every15minutes,
      s3Targets = List(
        aws.glue.inputs.CrawlerS3TargetArgs(
          path = p"s3://${eventsBucket.bucket}"
        )
      )
    )
  )

  // Create a Glue connection to the Redshift cluster.
  val glueRedshiftConnection = aws.glue.Connection(
    name = "glue-redshift-connection",
    aws.glue.ConnectionArgs(
      connectionType = "JDBC",
      connectionProperties = Map(
        "JDBC_CONNECTION_URL" -> p"jdbc:redshift://${cluster.endpoint}/${clusterDBName}",
        "USERNAME" -> clusterDBUsername,
        "PASSWORD" -> clusterDBPassword
      ),
      physicalConnectionRequirements = aws.glue.inputs.ConnectionPhysicalConnectionRequirementsArgs(
        securityGroupIdLists = cluster.vpcSecurityGroupIds,
        availabilityZone = subnet.availabilityZone,
        subnetId = subnet.id
      )
    )
  )

  // Create an S3 bucket for Glue scripts and temporary storage.
  val glueJobBucket = aws.s3.Bucket(
    name = "glue-job-bucket",
    aws.s3.BucketArgs(
      forceDestroy = true
    )
  )

  // Upload a Glue job script.
  val glueJobScript = aws.s3.BucketObject(
    name = "glue-job.py",
    aws.s3.BucketObjectArgs(
      bucket = glueJobBucket.id,
      source = Asset.FileAsset("./glue-job.py")
    )
  )

  // Create a Glue job that runs our Python ETL script.
  val glueJob = aws.glue.Job(
    name = "glue-job",
    aws.glue.JobArgs(
      roleArn = glueRole.arn,
      glueVersion = "3.0",
      numberOfWorkers = 10,
      workerType = "G.1X",
      defaultArguments = Map(
        // Enabling job bookmarks helps you avoid loading duplicate data.
        // https://docs.aws.amazon.com/glue/latest/dg/monitor-continuations.html
        "--job-bookmark-option" -> "job-bookmark-enable",
        "--ConnectionName" -> glueRedshiftConnection.name,
        "--GlueDBName" -> glueDBName,
        "--GlueDBTableName" -> eventsBucket.bucket.map(_.replace("-", "_")),
        "--RedshiftDBName" -> clusterDBName,
        "--RedshiftDBTableName" -> "events",
        "--RedshiftRoleARN" -> redshiftRole.arn,
        "--TempDir" -> p"s3://${glueJobBucket.bucket}/glue-job-temp"
      ),
      connections = List(glueRedshiftConnection.name),
      command = aws.glue.inputs.JobCommandArgs(
        scriptLocation = p"s3://${glueJobBucket.bucket}/glue-job.py",
        pythonVersion = "3"
      )
    )
  )

  // Create a Glue trigger to run the job every 15 minutes.
  val glueJobTrigger = aws.glue.Trigger(
    name = "trigger",
    aws.glue.TriggerArgs(
      schedule = every15minutes,
      `type` = "SCHEDULED",
      actions = List(
        aws.glue.inputs.TriggerActionArgs(jobName = glueJob.name)
      )
    )
  )

  Stack(vpcEndpoint, glueCrawler, glueJobScript, glueJobTrigger).exports(
    dataBucketName = eventsBucket.bucket
  )
}
