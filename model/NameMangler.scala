package besom.model

object NameMangler:
  private val anyRefMethodNames = Set(
    "eq",
    "ne",
    "notify",
    "notifyAll",
    "synchronized",
    "wait",
    "asInstanceOf",
    "clone",
    "equals",
    "getClass",
    "hashCode",
    "isInstanceOf",
    "toString",
    "finalize"
  )

  private val reservedMethods = Set(
    "pulumiResourceName",
    "asString",
    "map",
    "flatMap",
    "zip"
  )

  private val reservedPackages = Set(
    "java",
    "javax",
    "scala",
    "besom"
  )

  private val reserved        = anyRefMethodNames ++ reservedMethods ++ reservedPackages
  private val mangledReserved = reserved.map(name => name + "_")

  def isReserved(name: String): Boolean = reserved.contains(name)

  def manglePropertyName(name: String): String =
    if reserved.contains(name) then name + "_" // append an underscore to the name
    else name

  def unmanglePropertyName(name: String): String =
    if mangledReserved.contains(name) then name.dropRight(1) // drop the underscore
    else name

end NameMangler
