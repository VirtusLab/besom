package besom.util

import besom.types.*

class URNTest extends munit.FunSuite with CompileAssertions:

  // case class Sample(urn: String, )

  val exampleResourceString =
    "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"

  val exampleNestedResourceString = URN(
    "urn:pulumi:stack::project::some:happy:component$custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
  )

  val exampleStackString =
    "urn:pulumi:stack::project::pulumi:pulumi:Stack::stack-name"

  val shortResourceTypeUrnString =
    URN("urn:pulumi:stack::project::some:happy:component$custom:Resource$besom:Resource::my-test-resource") // two segments in resourceType

  val example = URN(
    "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
  )

  val exampleStack = URN(
    "urn:pulumi:stack::project::pulumi:pulumi:Stack::stack-name"
  )

  val kubernetesIngressUrn = URN(
    "urn:pulumi:stack::project::kubernetes:networking.k8s.io/v1:Ingress::my-ingress"
  )

  val doubleDottedResourceTypeUrn = URN(
    "urn:pulumi:stack::project::custom:resources.example.org:Resource$besom:testing.example.com/test:Resource::my-test-resource"
  )

  test("URN.apply should only work for correct URNs") {
    failsToCompile("""
      import besom.types.URN
      URN("well::it's::not::a::urn")
      """)

    compiles("""
      import besom.types.URN
      URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource")
      """)
  }

  test("runtime URN should parse correctly") {
    val maybeParsed = URN.from(exampleResourceString)
    assert(maybeParsed.isSuccess)
    assert(maybeParsed.get == example)
    assert(URN.from("wrong").isFailure)
  }

  test("URN should expose stack correctly") {
    assert(example.stack == "stack", s"Expected stack to be 'stack', got ${example.stack}")
  }

  test("URN should expose project correctly") {
    assert(example.project == "project", s"Expected project to be 'project', got ${example.project}")
  }

  test("URN should expose parentType correctly") {
    assert(
      example.parentType == Vector("custom:resources:Resource"),
      s"""Expected parentType to be 'Vector("custom:resources:Resource"), got ${example.parentType}"""
    )
  }

  test("URN should expose resourceType correctly") {
    assert(
      example.resourceType == "besom:testing/test:Resource",
      s"Expected resourceType to be 'besom:testing/test:Resource', got ${example.resourceType}"
    )
  }

  test("URN should expose resourceName correctly") {
    assert(
      example.resourceName == "my-test-resource",
      s"Expected resourceName to be 'my-test-resource', got ${example.resourceName}"
    )
  }

  test("URN should expose stack correctly for stacks") {
    assert(exampleStack.stack == "stack", s"Expected stack to be 'stack', got ${exampleStack.stack}")
  }

  test("URN should expose project correctly for stacks") {
    assert(exampleStack.project == "project", s"Expected project to be 'project', got ${exampleStack.project}")
  }

  test("URN should expose parentType correctly for stacks") {
    assert(
      exampleStack.parentType.isEmpty,
      s"Expected parentType to be empty, got ${exampleStack.parentType}"
    )
  }

  test("URN should expose resourceType correctly for stacks") {
    assert(
      exampleStack.resourceType == "pulumi:pulumi:Stack",
      s"Expected resourceType to be 'pulumi:pulumi:Stack', got ${exampleStack.resourceType}"
    )
  }

  test("URN should expose resourceName correctly for stacks") {
    assert(
      exampleStack.resourceName == "stack-name",
      s"Expected resourceName to be 'stack-name', got ${exampleStack.resourceName}"
    )
  }

  test("URN should expose parentType correctly for nested resources") {
    assert(
      exampleNestedResourceString.parentType == Vector("some:happy:component", "custom:resources:Resource"),
      s"""Expected parentType to be Vector("some:happy:component", "custom:resources:Resource"), got ${exampleNestedResourceString.parentType}"""
    )
  }

  test("URN should expose resourceType correctly for nested resources") {
    assert(
      exampleNestedResourceString.resourceType == "besom:testing/test:Resource",
      s"Expected resourceType to be 'besom:testing/test:Resource', got ${exampleNestedResourceString.resourceType}"
    )
  }

  test("URN should expose resourceName correctly for nested resources") {
    assert(
      exampleNestedResourceString.resourceName == "my-test-resource",
      s"Expected resourceName to be 'my-test-resource', got ${exampleNestedResourceString.resourceName}"
    )
  }

  test("URN should expose parentType correctly for short resource types") {
    assert(
      shortResourceTypeUrnString.parentType == Vector("some:happy:component", "custom:Resource"),
      s"""Expected parentType to be Vector("custom:resources:Resource"), got ${shortResourceTypeUrnString.parentType}"""
    )
  }

  test("URN should expose resourceType correctly for short resource types") {
    assert(
      shortResourceTypeUrnString.resourceType == "besom:Resource",
      s"Expected resourceType to be 'besom:Resource', got ${shortResourceTypeUrnString.resourceType}"
    )
  }

  test("URN should expose resourceName correctly for short resource types") {
    assert(
      shortResourceTypeUrnString.resourceName == "my-test-resource",
      s"Expected resourceName to be 'my-test-resource', got ${shortResourceTypeUrnString.resourceName}"
    )
  }

  test("URN should disallow dumb stuff at compile time") {
    // empty resource name, nope nope nope
    failsToCompile("""
      import besom.types.URN
      URN("urn:pulumi:stack::project::resource:type::")
      """)

    // this is tripped by newline in resource name
    failsToCompile("""
      import besom.types.URN
      URN("urn:pulumi:stack::project::resource:type::some really ::^&\n*():: crazy name")
      """)
  }

  List(
    "urn:pulumi:test::test::pulumi:pulumi:Stack::test-test",
    "urn:pulumi:stack-name::project-name::my:customtype$aws:s3/bucket:Bucket::bob",

    // these 3 are pure garbage, the resource type is non-compliant in all 3
    // they are duplicated with fixed resource types below
    // "urn:pulumi:stack::project::type::",
    // "urn:pulumi:stack::project::type::some really ::^&\n*():: crazy name",
    // "urn:pulumi:stack::project with whitespace::type::some name",

    "urn:pulumi:stack::project::resource:type::",
    "urn:pulumi:stack::project::resource:type::some really ::^&\n*():: crazy name",
    "urn:pulumi:stack::project with whitespace::resource:type::some name",
    "urn:pulumi:test::test::pkgA:index:t1-new$pkgA:index:t2::n1-new-sub", // had to add hyphen to Identifier to make it work
    "urn:pulumi:dev::iac-workshop::pulumi:pulumi:Stack::iac-workshop-dev",
    "urn:pulumi:dev::iac-workshop::apigateway:index:RestAPI::helloWorldApi",
    "urn:pulumi:dev::workshop::apigateway:index:RestAPI$aws:apigateway/restApi:RestApi::helloWorldApi",
    "urn:pulumi:dev::workshop::apigateway:index:RestAPI$aws:lambda/permission:Permission::helloWorldApi-fa520765",
    "urn:pulumi:stage::demo::eks:index:Cluster$pulumi:providers:kubernetes::eks-provider",
    "urn:pulumi:defStack::defProject::kubernetes:storage.k8s.io/v1beta1:CSIDriver::defName",
    "urn:pulumi:stack::project::my:my$aws:sns/topicSubscription:TopicSubscription::TopicSubscription",
    "urn:pulumi:foo::countdown::aws:cloudwatch/logSubscriptionFilter:LogSubscriptionFilter::countDown_watcher",
    "urn:pulumi:stack::project::pulumi:providers:aws::default_4_13_0",
    "urn:pulumi:foo::todo::aws:s3/bucketObject:BucketObject::todo4c238266/index.html",
    "urn:pulumi:dev::awsx-pulumi-issue::awsx:ec2:Vpc$aws:ec2/vpc:Vpc$aws:ec2/subnet:Subnet$aws:ec2/routeTable:RouteTable$aws:ec2/routeTableAssociation:RouteTableAssociation::example-private-vpc-public-1",
    "urn:pulumi:dev::eks::pulumi:providers:aws::default_4_36_0"
  ).foreach { u =>
    test(s"runtime URN should parse $u") {
      val maybeParsed = URN.from(u)
      assert(maybeParsed.isSuccess)
    }
  }

end URNTest
