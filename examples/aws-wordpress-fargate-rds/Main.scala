import besom.*
import besom.api.random

@main def main = Pulumi.run {
  val serviceName = "wp-fargate-rds"
  val dbName      = config.getString("dbName").getOrElse("wordpress")
  val dbUser      = config.getString("dbUser").getOrElse("admin")

  val dbPassword = config
    .getString("dbPassword")
    .getOrElse(
      random
        .RandomPassword(
          "dbPassword",
          random.RandomPasswordArgs(
            length = 16,
            special = true,
            overrideSpecial = "_%"
          )
        )
        .result
    )

  val vpc = AwsVpc(s"$serviceName-net")

  val db = Db(
    name = s"$serviceName-db",
    DbArgs(
      dbName = dbName,
      dbUser = dbUser,
      dbPassword = dbPassword,
      subnetIds = vpc.subnetIds,
      securityGroupIds = vpc.rdsSecurityGroupIds
    )
  )

  val fe = WebService(
    s"$serviceName-fe",
    WebServiceArgs(
      dbHost = db.dbAddress,
      dbPort = "3306",
      dbName = db.dbName,
      dbUser = db.dbUser,
      dbPassword = db.dbPassword,
      vpcId = vpc.vpcId,
      subnetIds = vpc.subnetIds,
      securityGroupIds = vpc.feSecurityGroupIds
    )
  )

  Stack.exports(
    webServiceUrl = p"http://${fe.dnsName}",
    ecsClusterName = fe.clusterName,
    databaseEndpoint = db.dbAddress,
    databaseUserName = db.dbUser,
    databasePassword = db.dbPassword
  )
}
