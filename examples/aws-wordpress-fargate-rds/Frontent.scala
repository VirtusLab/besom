import besom.*
import besom.api.{aws, awsx}
import besom.json.*

case class WebServiceArgs(
  dbHost: Input[String],
  dbName: Input[String],
  dbUser: Input[String],
  dbPassword: Input[String],
  dbPort: Input[String],
  vpcId: Input[String],
  subnetIds: Input[List[String]],
  securityGroupIds: Input[List[String]]
)

case class WebService(
  serviceId: Output[ResourceId],
  dnsName: Output[String],
  clusterName: Output[String]
)(using ComponentBase)
    extends ComponentResource
    derives RegistersOutputs

object WebService:
  extension (c: Output[WebService])
    def dnsName: Output[String]     = c.flatMap(_.dnsName)
    def clusterName: Output[String] = c.flatMap(_.clusterName)

  def apply(using
    Context
  )(name: NonEmptyString, args: WebServiceArgs, options: ComponentResourceOptions = ComponentResourceOptions()): Output[WebService] =
    component(name, "custom:resource:WebService", options) {
      val cluster = aws.ecs.Cluster(s"$name-ecs")

      val alb = aws.lb.LoadBalancer(
        name = s"$name-alb",
        aws.lb.LoadBalancerArgs(
          securityGroups = args.securityGroupIds,
          subnets = args.subnetIds
        )
      )

      val atg = aws.lb.TargetGroup(
        name = s"$name-tg",
        aws.lb.TargetGroupArgs(
          port = 80,
          protocol = "HTTP",
          targetType = "ip",
          vpcId = args.vpcId,
          healthCheck = aws.lb.inputs.TargetGroupHealthCheckArgs(
            healthyThreshold = 2,
            interval = 5,
            timeout = 4,
            protocol = "HTTP",
            matcher = "200-399"
          )
        )
      )

      val wl = aws.lb.Listener(
        name = s"$name-listener",
        aws.lb.ListenerArgs(
          loadBalancerArn = alb.arn,
          port = 80,
          defaultActions = List(
            aws.lb.inputs.ListenerDefaultActionArgs(
              `type` = "forward",
              targetGroupArn = atg.arn
            )
          )
        )
      )
      val role = aws.iam.Role(
        name = s"$name-task-role",
        aws.iam.RoleArgs(
          assumeRolePolicy = json"""{
                                     "Version": "2012-10-17",
                                     "Statement": [{
                                         "Sid": "",
                                         "Effect": "Allow",
                                         "Principal": {
                                             "Service": "ecs-tasks.amazonaws.com"
                                         },
                                         "Action": "sts:AssumeRole"
                                     }]
                                 }""".map(_.prettyPrint)
        )
      )

      val rpa = aws.iam.RolePolicyAttachment(
        name = s"$name-task-policy",
        aws.iam.RolePolicyAttachmentArgs(
          role = role.name,
          policyArn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
        )
      )

      val task = awsx.ecs.FargateTaskDefinition(
        name = s"$name-app-task",
        awsx.ecs.FargateTaskDefinitionArgs(
          container = awsx.ecs.inputs.TaskDefinitionContainerDefinitionArgs(
            name = s"$name-app-container",
            image = "wordpress",
            cpu = 256,
            memory = 512,
            environment = List(
              envValue(name = "WORDPRESS_DB_HOST", value = p"${args.dbHost}:${args.dbPort}"),
              envValue(name = "WORDPRESS_DB_NAME", value = args.dbName),
              envValue(name = "WORDPRESS_DB_USER", value = args.dbUser),
              envValue(name = "WORDPRESS_DB_PASSWORD", value = args.dbPassword)
            ),
            portMappings = List(
              awsx.ecs.inputs.TaskDefinitionPortMappingArgs(
                containerPort = 80,
                hostPort = 80,
                protocol = "tcp"
              )
            )
          )
        )
      )

      val service = awsx.ecs.FargateService(
        name = s"$name-app-svc",
        awsx.ecs.FargateServiceArgs(
          networkConfiguration = aws.ecs.inputs.ServiceNetworkConfigurationArgs(
            assignPublicIp = true,
            subnets = args.subnetIds,
            securityGroups = args.securityGroupIds
          ),
          loadBalancers = List(
            aws.ecs.inputs.ServiceLoadBalancerArgs(
              containerName = s"$name-app-container",
              containerPort = 80,
              targetGroupArn = atg.arn
            )
          ),
          cluster = cluster.arn,
          taskDefinition = task.taskDefinition.arn,
          desiredCount = 1
        ),
        opts = opts(dependsOn = List(wl, rpa))
      )

      WebService(serviceId = service.id, dnsName = alb.dnsName, clusterName = cluster.name)
    }
end WebService

private def envValue(name: String, value: Input[String])(using
  Context
): awsx.ecs.inputs.TaskDefinitionKeyValuePairArgs =
  awsx.ecs.inputs.TaskDefinitionKeyValuePairArgs(name = name, value = value)
