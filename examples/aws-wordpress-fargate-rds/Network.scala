import besom.*
import besom.api.aws

case class AwsVpcArgs(
  cidrBlock: Option[String] = None,
  instanceTenancy: Option[String] = None,
  enableDnsHostnames: Option[Boolean] = None,
  enableDnsSupport: Option[Boolean] = None
)

case class AwsVpc(
  vpcId: Output[String],
  subnetIds: Output[List[String]],
  rdsSecurityGroupIds: Output[List[String]],
  feSecurityGroupIds: Output[List[String]]
)(using ComponentBase)
    extends ComponentResource
    derives RegistersOutputs

object AwsVpc:
  extension (c: Output[AwsVpc])
    def vpcId: Output[String]                     = c.flatMap(_.vpcId)
    def subnetIds: Output[List[String]]           = c.flatMap(_.subnetIds)
    def rdsSecurityGroupIds: Output[List[String]] = c.flatMap(_.rdsSecurityGroupIds)
    def feSecurityGroupIds: Output[List[String]]  = c.flatMap(_.feSecurityGroupIds)

  def apply(using
    Context
  )(name: NonEmptyString, args: AwsVpcArgs = AwsVpcArgs(), options: ComponentResourceOptions = ComponentResourceOptions()): Output[AwsVpc] =
    component(name, "custom:resource:VPC", options) {
      val vpc = aws.ec2.Vpc(
        name = s"$name-vpc",
        aws.ec2.VpcArgs(
          cidrBlock = args.cidrBlock.getOrElse("10.100.0.0/16"),
          instanceTenancy = args.instanceTenancy.getOrElse("default"),
          enableDnsHostnames = args.enableDnsHostnames.getOrElse(true),
          enableDnsSupport = args.enableDnsSupport.getOrElse(true)
        )
      )

      val igw = aws.ec2.InternetGateway(
        name = s"$name-igw",
        aws.ec2.InternetGatewayArgs(vpcId = vpc.id)
      )

      val routeTable = aws.ec2.RouteTable(
        name = s"$name-rt",
        aws.ec2.RouteTableArgs(
          vpcId = vpc.id,
          routes = List(
            aws.ec2.inputs.RouteTableRouteArgs(cidrBlock = "0.0.0.0/0", gatewayId = igw.id)
          )
        )
      )

      val allZones = aws.getAvailabilityZones(aws.GetAvailabilityZonesArgs(state = "available"))
      val subnets = Output.sequence(
        (1 to 2)
          .map(i =>
            val az = allZones.zoneIds.map(_.apply(i))
            val vpcSubnet = aws.ec2.Subnet(
              name = s"$name-subnet-$i",
              aws.ec2.SubnetArgs(
                assignIpv6AddressOnCreation = false,
                vpcId = vpc.id,
                mapPublicIpOnLaunch = true,
                cidrBlock = s"10.100.$i.0/24",
                availabilityZoneId = az
              )
            )
            val routeTableAssociation = aws.ec2.RouteTableAssociation(
              name = s"vpc-route-table-assoc-$i",
              aws.ec2.RouteTableAssociationArgs(
                routeTableId = routeTable.id,
                subnetId = vpcSubnet.id
              )
            )
            routeTableAssociation.flatMap(_ => vpcSubnet.id)
          )
          .toList
      )
      val rdsSecurityGroup = aws.ec2.SecurityGroup(
        name = s"$name-rds-sg",
        aws.ec2.SecurityGroupArgs(
          vpcId = vpc.id,
          description = "Allow client access",
          ingress = List(
            aws.ec2.inputs.SecurityGroupIngressArgs(
              cidrBlocks = List("0.0.0.0/0"),
              fromPort = 3306,
              toPort = 3306,
              protocol = "tcp",
              description = "Allow RDS access"
            )
          ),
          egress = List(
            aws.ec2.inputs.SecurityGroupEgressArgs(
              protocol = "-1",
              fromPort = 0,
              toPort = 0,
              cidrBlocks = List("0.0.0.0/0")
            )
          )
        )
      )

      val feSecurityGroup = aws.ec2.SecurityGroup(
        name = s"$name-fe-sg",
        aws.ec2.SecurityGroupArgs(
          vpcId = vpc.id,
          description = "Allows all HTTP(s) traffic.",
          ingress = List(
            aws.ec2.inputs.SecurityGroupIngressArgs(
              cidrBlocks = List("0.0.0.0/0"),
              fromPort = 443,
              toPort = 443,
              protocol = "tcp",
              description = "Allow https"
            ),
            aws.ec2.inputs.SecurityGroupIngressArgs(
              cidrBlocks = List("0.0.0.0/0"),
              fromPort = 80,
              toPort = 80,
              protocol = "tcp",
              description = "Allow http"
            )
          ),
          egress = List(
            aws.ec2.inputs.SecurityGroupEgressArgs(
              protocol = "-1",
              fromPort = 0,
              toPort = 0,
              cidrBlocks = List("0.0.0.0/0")
            )
          )
        )
      )

      AwsVpc(
        vpcId = vpc.id,
        subnetIds = subnets,
        rdsSecurityGroupIds = rdsSecurityGroup.id.map(List(_)),
        feSecurityGroupIds = feSecurityGroup.id.map(List(_))
      )
    }
end AwsVpc
