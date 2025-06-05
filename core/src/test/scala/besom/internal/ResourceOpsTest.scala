package besom.internal

import besom.types.{Label, ResourceId, URN, ProviderType}
import besom.internal.RunResult.given
import besom.internal.RunOutput.*
import besom.internal.logging.BesomMDC
import besom.internal.logging.Key.LabelKey
import besom.aliases.Output

class ResourceOpsTest extends munit.FunSuite:
  import ResourceOpsTest.fixtures.*

  // resolveTransitiveDependencies adds a dependency on the given resource to the set of deps.
  //
  // The behavior of this method depends on whether or not the resource is a custom resource, a local component resource,
  // a remote component resource, a dependency resource, or a rehydrated component resource:
  //
  //   - Custom resources are added directly to the set, as they are "real" nodes in the dependency graph.
  //   - Local component resources act as aggregations of their descendents. Rather than adding the component resource
  //     itself, each child resource is added as a dependency.
  //   - Remote component resources are added directly to the set, as they naturally act as aggregations of their children
  //     with respect to dependencies: the construction of a remote component always waits on the construction of its
  //     children.
  //   - Dependency resources are added directly to the set.
  //   - Rehydrated component resources are added directly to the set.
  //
  // In other words, if we had:
  //
  //			     Comp1   -   Dep2
  //		     /   |   \
  //	  Cust1  Comp2  Remote1
  //			     /   \     \
  //		   Cust2  Comp3   Comp4
  //	      /        \      \
  //	  Cust3        Dep1    Cust4
  //
  // Then the transitively reachable resources of Comp1 will be [Cust1, Cust2, Dep1, Remote1, Dep2].
  // It will *not* include:
  // * Cust3 because it is a child of a custom resource
  // * Comp2 and Comp3 because they are a non-remote component resource
  // * Comp4 and Cust4 because Comp4 is a child of a remote component resource
  test("transitive dependency resolution algorithm") {
    given Context         = DummyContext().unsafeRunSync()
    given BesomMDC[Label] = BesomMDC[Label](LabelKey, Label.fromNameAndType("test", "pkg:test:test"))

    val resources = Resources().unsafeRunSync()
    val ops       = ResourceOps()

    val cust1Urn = URN(
      "urn:pulumi:stack::project::custom:resources:TestCustomResource::cust1"
    )
    val cust1 = TestCustomResource(Output(cust1Urn), Output(ResourceId("cust1")), Output(1))

    val addCust1ToResources =
      resources.add(
        cust1,
        CustomResourceState(
          CommonResourceState(
            children = Set.empty, // leaf node
            provider = None,
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "cust1",
            typ = "custom:resources:TestCustomResource",
            transformations = List.empty,
            keepDependency = false // custom resources never have keepDependency set to true
          ),
          Output(ResourceId("cust1"))
        )
      )

    val cust3Urn = URN(
      "urn:pulumi:stack::project::custom:resources:TestCustomResource::cust3"
    )
    val cust3 = TestCustomResource(Output(cust3Urn), Output(ResourceId("cust3")), Output(3))

    val addCust3ToResources =
      resources.add(
        cust3,
        CustomResourceState(
          CommonResourceState(
            children = Set.empty, // leaf node
            provider = None,
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "cust3",
            typ = "custom:resources:TestCustomResource",
            transformations = List.empty,
            keepDependency = false // custom resources never have keepDependency set to true
          ),
          Output(ResourceId("cust3"))
        )
      )

    val cust4 =
      val urn = URN(
        "urn:pulumi:stack::project::custom:resources:TestCustomResource::cust4"
      )
      TestCustomResource(Output(urn), Output(ResourceId("cust4")), Output(4))

    val addCust4ToResources =
      resources.add(
        cust4,
        CustomResourceState(
          CommonResourceState(
            children = Set.empty, // leaf node
            provider = None,
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "cust4",
            typ = "custom:resources:TestCustomResource",
            transformations = List.empty,
            keepDependency = false // custom resources never have keepDependency set to true
          ),
          Output(ResourceId("cust4"))
        )
      )

    val cust2Urn = URN(
      "urn:pulumi:stack::project::custom:resources:TestCustomResource::cust2"
    )
    val cust2 = TestCustomResource(Output(cust2Urn), Output(ResourceId("cust2")), Output(2))

    val addCust2ToResources =
      resources.add(
        cust2,
        CustomResourceState(
          CommonResourceState(
            children = Set(cust3), // cust2 has a child cust3
            provider = None,
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "cust2",
            typ = "custom:resources:TestCustomResource",
            transformations = List.empty,
            keepDependency = false // custom resources never have keepDependency set to true
          ),
          Output(ResourceId("cust2"))
        )
      )

    val dep1Urn = URN(
      "urn:pulumi:stack::project::custom:resources:TestCustomResource::dep1"
    )
    val dep1 = DependencyResource(
      Output(
        dep1Urn
      )
    )
    // DependencyResource does not have a state (it's just a urn container), so we don't need to add it to the resources

    val comp3 = ComponentBase(
      Output(
        URN(
          "urn:pulumi:stack::project::component:resources:TestComponentResource::comp3"
        )
      )
    )

    val addComp3ToResources =
      resources.add(
        comp3,
        ComponentResourceState(
          CommonResourceState(
            children = Set(dep1), // comp3 has a child dep1
            provider = None,
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "comp3",
            typ = "component:resources:TestComponentResource",
            transformations = List.empty,
            keepDependency = false // this is a user component, so it should not have keepDependency set to true
          )
        )
      )

    val comp4 = ComponentBase(
      Output(
        URN(
          "urn:pulumi:stack::project::component:resources:TestComponentResource::comp4"
        )
      )
    )

    val addComp4ToResources =
      resources.add(
        comp4,
        ComponentResourceState(
          CommonResourceState(
            children = Set(cust4), // comp4 has a child cust4
            provider = None,
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "comp4",
            typ = "component:resources:TestComponentResource",
            transformations = List.empty,
            keepDependency = false // this is a user component, so it should not have keepDependency set to true
          )
        )
      )

    val remote1Urn = URN(
      "urn:pulumi:stack::project::component:resources:TestRemoteComponentResource::remote1"
    )
    val remote1 = TestRemoteComponentResource(
      Output(
        remote1Urn
      ),
      Output("remote1")
    )

    val addRemote1ToResources =
      resources.add(
        remote1,
        ComponentResourceState(
          CommonResourceState(
            children = Set(comp4), // remote1 has a child comp4
            provider = None,
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "remote1",
            typ = "component:resources:TestRemoteComponentResource",
            transformations = List.empty,
            keepDependency = true // remote component resources always have keepDependency set to true
          )
        )
      )

    val comp2 = ComponentBase(
      Output(
        URN(
          "urn:pulumi:stack::project::component:resources:TestComponentResource::comp2"
        )
      )
    )

    val addComp2ToResources = resources.add(
      comp2,
      ComponentResourceState(
        CommonResourceState(
          children = Set(cust2, comp3), // comp2 has children cust2 and comp3
          provider = None,
          providers = Map.empty,
          version = "0.0.1",
          pluginDownloadUrl = "",
          name = "comp2",
          typ = "component:resources:TestComponentResource",
          transformations = List.empty,
          keepDependency = false // this is a user component, so it should not have keepDependency set to true
        )
      )
    )

    val dep2Urn = URN(
      "urn:pulumi:stack::project::custom:resources:TestCustomResource::dep2"
    )
    val dep2 = DependencyResource(
      Output(
        dep2Urn
      )
    )

    val comp1 = ComponentBase(
      Output(
        URN(
          "urn:pulumi:stack::project::component:resources:TestComponentResource::root"
        )
      )
    )

    val addComp1ToResources = resources
      .add(
        comp1,
        ComponentResourceState(
          CommonResourceState(
            children = Set(cust1, comp2, remote1, dep2), // comp1 has children cust1, comp2, remote1 and dep2
            provider = None,
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "comp1",
            typ = "component:resources:TestCustomResource",
            transformations = List.empty,
            keepDependency = false // this is a user component, so it should not have keepDependency set to true
          )
        )
      )

    val transitiveDeps =
      (for // add in the same order as declared
        _    <- addCust1ToResources
        _    <- addCust3ToResources
        _    <- addCust4ToResources
        _    <- addCust2ToResources
        _    <- addComp3ToResources
        _    <- addComp4ToResources
        _    <- addRemote1ToResources
        _    <- addComp2ToResources
        _    <- addComp1ToResources
        deps <- ops.resolveTransitiveDependencies(Set(comp1), resources)
      yield deps).unsafeRunSync()

    assertEquals(transitiveDeps, Set(cust1Urn, cust2Urn, dep1Urn, dep2Urn, remote1Urn))
  }

  test("resource provider getters") {
    object syntax extends BesomSyntax
    val resources = Resources().unsafeRunSync()
    given Context = DummyContext(resources = resources).unsafeRunSync()
    // given BesomMDC[Label] = BesomMDC[Label](LabelKey, Label.fromNameAndType("test", "pkg:test:test"))

    val providerRes = TestProviderResource(
      Output(
        URN(
          "urn:pulumi:stack::project::provider:resources:TestProviderResource::provider"
        )
      ),
      Output(ResourceId("provider")),
      Output("provider")
    )

    resources
      .add(
        providerRes,
        ProviderResourceState(
          CustomResourceState(
            CommonResourceState(
              children = Set.empty,
              provider = None,
              providers = Map.empty,
              version = "0.0.1",
              pluginDownloadUrl = "",
              name = "provider",
              typ = "pulumi:providers:TestProviderResource",
              transformations = List.empty,
              keepDependency = false // providers never have keepDependency set to true
            ),
            Output(ResourceId("provider"))
          ),
          ProviderType.from("pulumi:providers:TestProviderResource").getPackage
        )
      )
      .unsafeRunSync()

    val cust1Urn = URN(
      "urn:pulumi:stack::project::custom:resources:TestCustomResource::cust1"
    )
    val cust1 = TestCustomResource(Output(cust1Urn), Output(ResourceId("cust1")), Output(1))

    resources
      .add(
        cust1,
        CustomResourceState(
          CommonResourceState(
            children = Set.empty,
            provider = Some(providerRes),
            providers = Map.empty,
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "cust1",
            typ = "custom:resources:TestCustomResource",
            transformations = List.empty,
            keepDependency = false // custom resources never have keepDependency set to true
          ),
          Output(ResourceId("cust1"))
        )
      )
      .unsafeRunSync()

    val comp1Urn = URN(
      "urn:pulumi:stack::project::component:resources:TestComponentResource::comp1"
    )
    val compBase1 = ComponentBase(
      Output(comp1Urn)
    )

    val comp1 = TestComponentResource(
      Output("comp1")
    )(using compBase1)

    resources
      .add(
        compBase1,
        ComponentResourceState(
          CommonResourceState(
            children = Set.empty,
            provider = None,
            providers = Map(
              "provider" -> providerRes
            ),
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "comp1",
            typ = "component:resources:TestComponentResource",
            transformations = List.empty,
            keepDependency = false
          )
        )
      )
      .unsafeRunSync()

    val remote1Urn = URN(
      "urn:pulumi:stack::project::component:resources:TestRemoteComponentResource::remote1"
    )

    val remote1 = TestRemoteComponentResource(
      Output(remote1Urn),
      Output("remote1")
    )

    resources
      .add(
        remote1,
        ComponentResourceState(
          CommonResourceState(
            children = Set.empty,
            provider = None,
            providers = Map(
              "provider" -> providerRes
            ),
            version = "0.0.1",
            pluginDownloadUrl = "",
            name = "remote1",
            typ = "component:resources:TestRemoteComponentResource",
            transformations = List.empty,
            keepDependency = true
          )
        )
      )
      .unsafeRunSync()

    import syntax.{provider, providers}

    providerRes.provider.unsafeRunSync().get match
      case Some(_) => fail("providerRes.provider should be None")
      case None    =>

    cust1.provider.unsafeRunSync().get match
      case Some(p) =>
        assert(p == providerRes)
      case None =>
        fail("cust1.provider should be Some(providerRes)")

    comp1.providers.unsafeRunSync().get match
      case m if m.isEmpty =>
        fail("comp1.providers should not be empty")
      case m =>
        assertEquals(m, Map("provider" -> providerRes))

    remote1.providers.unsafeRunSync().get match
      case m if m.isEmpty =>
        fail("remote1.providers should not be empty")
      case m =>
        assertEquals(m, Map("provider" -> providerRes))
  }

end ResourceOpsTest

object ResourceOpsTest:
  object fixtures:
    case class TestRemoteComponentResource(urn: Output[URN], str: Output[String]) extends RemoteComponentResource

    case class TestCustomResource(urn: Output[URN], id: Output[ResourceId], bump: Output[Int]) extends CustomResource

    case class TestComponentResource(str: Output[String])(using ComponentBase) extends ComponentResource

    case class TestProviderResource(urn: Output[URN], id: Output[ResourceId], str: Output[String]) extends ProviderResource

    def prepareTransitiveDependencyResolutionTree(resources: Resources): Result[Unit] = ???
