package besom.codegen

import besom.codegen.Config.ProviderConfig
import besom.codegen.SchemaName

import scala.meta.*
import scala.meta.dialects.Scala33

//noinspection ScalaFileName,TypeAnnotation
class PulumiDefinitionCoordinatesTest extends munit.FunSuite {
  import besom.codegen.metaschema.{Java, Language, Meta, PulumiPackage}

  implicit val logger: Logger = new Logger

  val schemaDir = os.pwd / ".out" / "schemas"

  case class Data(
    schemaName: SchemaName,
    typeToken: String,
    meta: Meta = Meta(),
    language: Language = Language(),
    tags: Set[munit.Tag] = Set()
  )(val expected: Expectations*) {
    val pulumiPackage = PulumiPackage(name = schemaName, meta = meta, language = language)
  }

  sealed trait Expectations {
    def fullPackageName: String
    def fullyQualifiedTypeRef: String
    def filePath: String
  }

  case class ResourceClassExpectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String,
    asArgsType: Boolean = false
  ) extends Expectations

  case class ObjectClassExpectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String,
    asArgsType: Boolean = false
  ) extends Expectations

  case class EnumClassExpectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String
  ) extends Expectations

  case class FunctionClassExpectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String,
    args: FunctionClassExpectations.FunctionClassExpectationsArgs,
    result: FunctionClassExpectations.FunctionClassExpectationsResult
  ) extends Expectations
  object FunctionClassExpectations {
    case class FunctionClassExpectationsArgs(
      fullPackageName: String,
      fullyQualifiedTypeRef: String,
      filePath: String
    )
    case class FunctionClassExpectationsResult(
      fullPackageName: String,
      fullyQualifiedTypeRef: String,
      filePath: String
    )
  }

  val tests = List(
    Data(
      schemaName = "digitalocean",
      typeToken = "digitalocean:index:Domain",
      meta = Meta(
        moduleFormat = "(.*)(?:/[^/]*)"
      )
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.digitalocean",
        fullyQualifiedTypeRef = "besom.api.digitalocean.Domain",
        filePath = "src/index/Domain.scala"
      )
    ),
    Data(
      schemaName = "kubernetes",
      typeToken = "pulumi:providers:kubernetes"
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.kubernetes",
        fullyQualifiedTypeRef = "besom.api.kubernetes.Provider",
        filePath = "src/index/Provider.scala"
      )
    ),
    Data(
      schemaName = "digitalocean",
      typeToken = "digitalocean:index/getProjectsProject:getProjectsProject",
      meta = Meta(
        moduleFormat = "(.*)(?:/[^/]*)"
      )
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.digitalocean",
        fullyQualifiedTypeRef = "besom.api.digitalocean.GetProjectsProject",
        filePath = "src/index/GetProjectsProject.scala"
      )
    ),
    Data(
      schemaName = "foo-bar",
      typeToken = "foo-bar:index:TopLevel"
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.foobar",
        fullyQualifiedTypeRef = "besom.api.foobar.TopLevel",
        filePath = "src/index/TopLevel.scala"
      )
    ),
    Data(
      schemaName = "kubernetes",
      typeToken = "kubernetes:meta/v1:APIVersions"
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.kubernetes.meta.v1",
        fullyQualifiedTypeRef = "besom.api.kubernetes.meta.v1.ApiVersions",
        filePath = "src/meta/v1/ApiVersions.scala"
      )
    ),
    Data(
      schemaName = "kubernetes",
      typeToken = "kubernetes:authentication.k8s.io/v1:TokenRequest",
      language = Language(
        java = Java(
          packages = Map(
            "authentication.k8s.io/v1" -> "authentication.v1"
          )
        )
      )
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.kubernetes.authentication.v1",
        fullyQualifiedTypeRef = "besom.api.kubernetes.authentication.v1.TokenRequest",
        filePath = "src/authentication/v1/TokenRequest.scala"
      )
    ),
    Data(
      schemaName = "kubernetes",
      typeToken = "kubernetes:rbac.authorization.k8s.io/v1:ClusterRoleBinding",
      language = Language(
        java = Java(
          packages = Map(
            "rbac.authorization.k8s.io/v1" -> "rbac.authorization.v1"
          )
        )
      )
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.kubernetes.rbac.authorization.v1",
        fullyQualifiedTypeRef = "besom.api.kubernetes.rbac.authorization.v1.ClusterRoleBinding",
        filePath = "src/rbac/authorization/v1/ClusterRoleBinding.scala"
      )
    ),
    Data(
      schemaName = "aws",
      typeToken = "aws:ec2/getAmi:getAmi",
      meta = Meta(
        moduleFormat = "(.*)(?:/[^/]*)"
      )
    )(
      FunctionClassExpectations(
        fullPackageName = "besom.api.aws.ec2",
        fullyQualifiedTypeRef = "besom.api.aws.ec2.getAmi",
        filePath = "src/ec2/getAmi.scala",
        args = FunctionClassExpectations.FunctionClassExpectationsArgs(
          fullPackageName = "besom.api.aws.ec2",
          fullyQualifiedTypeRef = "besom.api.aws.ec2.GetAmiArgs",
          filePath = "src/ec2/GetAmiArgs.scala"
        ),
        result = FunctionClassExpectations.FunctionClassExpectationsResult(
          fullPackageName = "besom.api.aws.ec2",
          fullyQualifiedTypeRef = "besom.api.aws.ec2.GetAmiResult",
          filePath = "src/ec2/GetAmiResult.scala"
        )
      )
    ),
    Data(
      schemaName = "eks",
      typeToken = "eks:index:Cluster/getKubeconfig",
      meta = Meta(
        moduleFormat = "(.*)"
      )
    )(
      FunctionClassExpectations(
        fullPackageName = "besom.api.eks",
        fullyQualifiedTypeRef = "besom.api.eks.Cluster.getKubeconfig",
        // this will not be used actually because it is a class method and not a standalone function
        // but we include this anyway to document the current behaviour
        filePath = "src/index/Cluster.scala",
        args = FunctionClassExpectations.FunctionClassExpectationsArgs(
          fullPackageName = "besom.api.eks",
          fullyQualifiedTypeRef = "besom.api.eks.ClusterGetKubeconfigArgs",
          filePath = "src/index/ClusterGetKubeconfigArgs.scala"
        ),
        result = FunctionClassExpectations.FunctionClassExpectationsResult(
          fullPackageName = "besom.api.eks",
          fullyQualifiedTypeRef = "besom.api.eks.ClusterGetKubeconfigResult",
          filePath = "src/index/ClusterGetKubeconfigResult.scala"
        )
      )
    )
  )

  tests.foreach { data =>
    test(s"Type: ${data.typeToken}".withTags(data.tags)) {
      implicit val providerConfig: ProviderConfig = Config.providersConfigs(data.schemaName)

      val schemaProvider: SchemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
      val (_, packageInfo)               = schemaProvider.packageInfo(PackageMetadata(data.schemaName), data.pulumiPackage)

      val coords = PulumiToken(data.typeToken).toCoordinates(packageInfo)

      data.expected.foreach {
        case ResourceClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, asArgsType) =>
          val rc = coords.asResourceClass(asArgsType = asArgsType)
          assertEquals(rc.packageRef.syntax, fullPackageName)
          assertEquals(rc.typeRef.syntax, fullyQualifiedTypeRef)
          assertEquals(rc.filePath.osSubPath.toString(), filePath)
        case ObjectClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, asArgsType) =>
          val oc = coords.asObjectClass(asArgsType = asArgsType)
          assertEquals(oc.packageRef.syntax, fullPackageName)
          assertEquals(oc.typeRef.syntax, fullyQualifiedTypeRef)
          assertEquals(oc.filePath.osSubPath.toString(), filePath)
        case EnumClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath) =>
          val ec = coords.asEnumClass
          assertEquals(ec.packageRef.syntax, fullPackageName)
          assertEquals(ec.typeRef.syntax, fullyQualifiedTypeRef)
          assertEquals(ec.filePath.osSubPath.toString(), filePath)
        case FunctionClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, args, result) =>
          val m = coords.resourceMethod
          assertEquals(m.packageRef.syntax, fullPackageName)
          assertEquals(m.typeRef.syntax, fullyQualifiedTypeRef)
          assertEquals(m.filePath.osSubPath.toString(), filePath)
          val ac = coords.methodArgsClass
          assertEquals(ac.packageRef.syntax, args.fullPackageName)
          assertEquals(ac.typeRef.syntax, args.fullyQualifiedTypeRef)
          assertEquals(ac.filePath.osSubPath.toString(), args.filePath)
          val rc = coords.methodResultClass
          assertEquals(rc.packageRef.syntax, result.fullPackageName)
          assertEquals(rc.typeRef.syntax, result.fullyQualifiedTypeRef)
          assertEquals(rc.filePath.osSubPath.toString(), result.filePath)
      }
    }
  }

  test("normalize strings correctly") {
    val testCases = Map(
      "A" -> "A",
      "ID" -> "Id",
      "JSONPatch" -> "JsonPatch",
      "URLParser" -> "UrlParser",
      "camelCase" -> "CamelCase",
      "PascalCase" -> "PascalCase",
      "lowercase" -> "Lowercase",
      "UPPERCASE" -> "Uppercase",
      "ListenerXForwardedForConfig" -> "ListenerxForwardedForConfig"
    )

    for ((input, expected) <- testCases) {
      assertEquals(PulumiDefinitionCoordinates.normalize(input), expected)
    }
  }
}
