import besom.*
import besom.api.aws
import besom.json.*

@main def main = Pulumi.run {
  val eventSource = "my-event-source"

  // Create an HTTP API.
  val api = aws.apigatewayv2.Api(
    name = "api",
    aws.apigatewayv2.ApiArgs(protocolType = "HTTP")
  )

  // Create a stage and set it to deploy automatically.
  val stage = aws.apigatewayv2.Stage(
    name = "stage",
    aws.apigatewayv2.StageArgs(
      apiId = api.id,
      name = pulumiStack,
      autoDeploy = true
    )
  )

  // Create an event bus.
  val bus = aws.cloudwatch.EventBus("bus")

  // Create an event rule to watch for events.
  val rule = aws.cloudwatch.EventRule(
    name = "rule",
    aws.cloudwatch.EventRuleArgs(
      eventBusName = bus.name,
      // Specify the event pattern to watch for.
      eventPattern = json"""{ "source": [$eventSource] }""".map(_.prettyPrint)
    )
  )

  // Define a policy granting API Gateway permission to publish to EventBridge.
  val apiGatewayRole = aws.iam.Role(
    name = "api-gateway-role",
    aws.iam.RoleArgs(
      assumeRolePolicy = json"""{
                                     "Version": "2012-10-17",
                                     "Statement": [{
                                         "Effect": "Allow",
                                         "Principal": {
                                             "Service": "apigateway.amazonaws.com"
                                         },
                                         "Action": "sts:AssumeRole"
                                     }]
                                 }""".map(_.prettyPrint),
      managedPolicyArns = List(aws.iam.enums.ManagedPolicy.AmazonEventBridgeFullAccess.value)
    )
  )

  // Create an API Gateway integration to forward requests to EventBridge
  val integration = aws.apigatewayv2.Integration(
    name = "integration",
    aws.apigatewayv2.IntegrationArgs(
      apiId = api.id,
      // The integration type and subtype.
      integrationType = "AWS_PROXY",
      integrationSubtype = "EventBridge-PutEvents",
      credentialsArn = apiGatewayRole.arn,

      // The body of the request to be sent to EventBridge. Note the
      // event source matches pattern defined on the EventRule, and the
      // Detail expression, which just forwards the body of the original
      // API Gateway request (i.e., the uploaded document).
      requestParameters = Map(
        "EventBusName" -> bus.name,
        "Source" -> eventSource,
        "DetailType" -> "my-detail-type",
        "Detail" -> "$request.body"
      )
    )
  )

  // Finally, define the route.
  val route = aws.apigatewayv2.Route(
    name = "route",
    aws.apigatewayv2.RouteArgs(
      apiId = api.id,
      routeKey = "POST /uploads",
      target = p"integrations/${integration.id}"
    )
  )

  // Create an AWS IAM role for the Lambda function
  val lambdaRole = aws.iam.Role(
    name = "lambdaRole",
    aws.iam.RoleArgs(
      assumeRolePolicy = json"""{
                                      "Version": "2012-10-17",
                                      "Statement": [{
                                          "Effect": "Allow",
                                          "Principal": {
                                              "Service": "lambda.amazonaws.com"
                                          },
                                          "Action": "sts:AssumeRole"
                                      }]
                                  }""".map(_.prettyPrint),
      managedPolicyArns = List(aws.iam.enums.ManagedPolicy.AWSLambdaBasicExecutionRole.value)
    )
  )

  // For now, just log the event, including the uploaded document.
  // That'll be enough to verify everything's working.
  val lambdaCode =
    """exports.handler = async (event) => {
      |  console.log({ source: event.source, detail: event.detail });
      |};""".stripMargin

  // Create a Lambda function handler with permission to log to CloudWatch.
  val lambda = aws.lambda.Function(
    name = "lambda",
    aws.lambda.FunctionArgs(
      role = lambdaRole.arn,
      runtime = aws.lambda.enums.Runtime.NodeJS18dX,
      handler = "index.handler",
      code = Archive.AssetArchive(Map("index.js" -> Asset.StringAsset(lambdaCode)))
    )
  )

  // Create an EventBridge target associating the event rule with the function.
  val lambdaTarget = aws.cloudwatch.EventTarget(
    name = "lambda-target",
    aws.cloudwatch.EventTargetArgs(
      arn = lambda.arn,
      rule = rule.name,
      eventBusName = bus.name
    )
  )

  // Give EventBridge permission to invoke the function.
  val lambdaPermission = aws.lambda.Permission(
    name = "lambda-permission",
    aws.lambda.PermissionArgs(
      action = "lambda:InvokeFunction",
      principal = "events.amazonaws.com",
      function = lambda.arn,
      sourceArn = rule.arn
    )
  )

  Stack(lambdaTarget, lambdaPermission, route).exports(
    url = p"${api.apiEndpoint}/${stage.name}"
  )
}
