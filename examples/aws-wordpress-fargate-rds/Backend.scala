import besom.*
import besom.api.aws

case class DbArgs(
  dbName: Input[String],
  dbUser: Input[String],
  dbPassword: Input[String],
  subnetIds: Input[List[String]],
  securityGroupIds: Input[List[String]]
)

case class Db(
  dbAddress: Output[String],
  dbName: Output[String],
  dbUser: Output[String],
  dbPassword: Output[String]
)(using ComponentBase)
    extends ComponentResource
    derives RegistersOutputs

object Db:
  extension (c: Output[Db])
    def dbAddress: Output[String]  = c.flatMap(_.dbAddress)
    def dbName: Output[String]     = c.flatMap(_.dbName)
    def dbUser: Output[String]     = c.flatMap(_.dbUser)
    def dbPassword: Output[String] = c.flatMap(_.dbPassword)

  def apply(using Context)(name: NonEmptyString, args: DbArgs, options: ComponentResourceOptions = ComponentResourceOptions()): Output[Db] =
    component(name, "custom:resource:DB", options) {

      val rdsSubnetGroup = aws.rds.SubnetGroup(
        name = s"$name-sng",
        aws.rds.SubnetGroupArgs(subnetIds = args.subnetIds)
      )

      val db = aws.rds.Instance(
        name = s"$name-rds",
        aws.rds.InstanceArgs(
          dbName = args.dbName,
          username = args.dbUser,
          password = args.dbPassword,
          vpcSecurityGroupIds = args.securityGroupIds,
          dbSubnetGroupName = rdsSubnetGroup.name,
          allocatedStorage = 20,
          engine = "mysql",
          engineVersion = "5.7",
          instanceClass = aws.rds.enums.InstanceType.T3_Micro,
          storageType = aws.rds.enums.StorageType.GP2,
          skipFinalSnapshot = true,
          publiclyAccessible = false
        )
      )

      Db(dbAddress = db.address, dbName = db.dbName, dbUser = db.username, dbPassword = db.password.map(_.get))
    }
