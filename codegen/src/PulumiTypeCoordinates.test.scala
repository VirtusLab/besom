package besom.codegen

//noinspection ScalaFileName,TypeAnnotation
class PulumiTypeCoordinatesTest extends munit.FunSuite {
  implicit val logger: Logger                        = new Logger
  implicit val providerConfig: Config.ProviderConfig = Config.ProviderConfig()

  case class Data(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    typeName: String,
    tags: munit.Tag*
  )(val expected: Expectations*)
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

  val tests = List(
    Data(
      providerPackageParts = Seq("example"),
      modulePackageParts = Seq(),
      typeName = "Provider"
    )(expected =
      ResourceClassExpectations(
        fullPackageName = "besom.api.example",
        fullyQualifiedTypeRef = "besom.api.example.Provider",
        filePath = "src/index/Provider.scala"
      )
    ),
    Data(
      providerPackageParts = Seq("example"),
      modulePackageParts = Seq("index"),
      typeName = "Provider"
    )(expected =
      ResourceClassExpectations(
        fullPackageName = "besom.api.example",
        fullyQualifiedTypeRef = "besom.api.example.Provider",
        filePath = "src/index/Provider.scala"
      )
    ),
    Data(
      providerPackageParts = Seq("kubernetes"),
      modulePackageParts = Seq("rbac", "v1beta1"),
      typeName = "RoleRef"
    )(expected =
      ObjectClassExpectations(
        fullPackageName = "besom.api.kubernetes.rbac.v1beta1.outputs",
        fullyQualifiedTypeRef = "besom.api.kubernetes.rbac.v1beta1.outputs.RoleRef",
        filePath = "src/rbac/v1beta1/outputs/RoleRef.scala"
      )
    )
  )

  tests.foreach(data => {
    test(s"Type: ${data.typeName}".withTags(data.tags.toSet)) {
      val ptc = PulumiTypeCoordinates(
        providerPackageParts = data.providerPackageParts,
        modulePackageParts = data.modulePackageParts,
        typeName = data.typeName
      )

      data.expected.foreach {
        case ResourceClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, asArgsType) =>
          val rc = ptc.asResourceClass(asArgsType = asArgsType)
          assertEquals(rc.fullPackageName, fullPackageName)
          assertEquals(rc.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(rc.filePath.osSubPath.toString(), filePath)
        case ObjectClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, asArgsType) =>
          val oc = ptc.asObjectClass(asArgsType = asArgsType)
          assertEquals(oc.fullPackageName, fullPackageName)
          assertEquals(oc.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(oc.filePath.osSubPath.toString(), filePath)
        case EnumClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath) =>
          val ec = ptc.asEnumClass
          assertEquals(ec.fullPackageName, fullPackageName)
          assertEquals(ec.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(ec.filePath.osSubPath.toString(), filePath)
      }
    }
  })
}
