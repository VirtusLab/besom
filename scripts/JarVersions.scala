package besom.scripts

import java.io.InputStream
import java.nio.file.{Files, Paths}
import java.util.zip.ZipInputStream
import scala.util.boundary, boundary.break
import ox.*

object JarVersions:

  val excludedJars = Vector("-javadoc.jar", "-sources.jar")

  private def readN(is: InputStream, n: Int): Array[Byte] =
    val buf = new Array[Byte](n)
    var off = 0
    while off < n do
      val r = is.read(buf, off, n - off)
      if r == -1 then throw Exception("Unexpected EOF in class header")
      off += r
    buf

  /** Returns classfile major version. Verifies CAFEBABE magic. */
  private def classMajor(is: InputStream): Int =
    val hdr = readN(is, 8) // 0..3 magic, 4..5 minor, 6..7 major
    val magic = java.lang.Integer.toUnsignedLong(
      ((hdr(0) & 0xff) << 24) | ((hdr(1) & 0xff) << 16) | ((hdr(2) & 0xff) << 8) | (hdr(3) & 0xff)
    )
    if magic != 0xcafebabeL then throw Exception(f"Bad magic: 0x$magic%08X")
    ((hdr(6) & 0xff) << 8) | (hdr(7) & 0xff)

  /** Returns the Java classfile version from a JAR, or an error message. */
  def getVersion(jarPath: String): Either[String, Int] =
    boundary:
      val zis = ZipInputStream(Files.newInputStream(Paths.get(jarPath)))
      try
        var foundVersion: Option[Int] = None
        var entry = zis.getNextEntry
        while entry != null do
          val name = entry.getName
          if name.endsWith(".class") then
            try
              val major = try classMajor(zis) catch 
                case e: Exception => break(Left(s"Cannot read $name: ${e.getMessage}")) 
              
              val inMr = name.startsWith("META-INF/versions/")
              
              if inMr then
                // Skip MRJAR versions - we only care about base classes
                zis.closeEntry()
                entry = zis.getNextEntry
              else
                // This is a base class
                foundVersion match
                  case Some(existingVersion) =>
                    if existingVersion != major then
                      break(Left(s"Multiple versions found: $existingVersion and $major in $name"))
                  case None =>
                    foundVersion = Some(major)
            catch
              case e: Throwable =>
                break(Left(s"Cannot read $name: ${e.getMessage}"))
          // move on; we only read 8 bytes; closing entry is fine
          zis.closeEntry()
          entry = zis.getNextEntry
        
        foundVersion match
          case Some(version) => Right(version)
          case None => Left(s"No class files found in JAR at $jarPath")
      finally zis.close()

  def wrapModuleName(moduleName: String): String =
    s"besom-${moduleName}_3.jar"

  enum Result:
    case Skipped
    case NotJar
    case Failed(err: String)
    case Success

  def checkVersionsRecursively(path: os.Path, expectedMaxVersion: Int, exclude: String*): Either[(Vector[String], Int), Int] =
    val excludedModules = exclude.map(wrapModuleName).toSet
    val paths = os.walk(path).toVector
    val results = paths.mapPar(Runtime.getRuntime.availableProcessors()) { path =>
      val last = path.last
      if excludedJars.exists(e => last.endsWith(e)) then Result.Skipped
      else if path.ext == "jar" && excludedModules.contains(last) then 
        println(s"Skipping $path because it's listed as excluded")
        Result.Skipped
      else if path.ext == "jar" then
        getVersion(path.toString) match
          case Left(error) =>
            Result.Failed(error)
          case Right(version) =>
            if version > expectedMaxVersion then
              Result.Failed(s"Expected version $expectedMaxVersion, but got $version in $path")
            else Result.Success
      else Result.NotJar
    }

    val errs = results.collect {
      case Result.Failed(error) => error
    }

    val jarCount = results.collect {
      case Result.Success => 1
      case Result.NotJar => 0
      case Result.Skipped => 1
      case Result.Failed(_) => 1
    }.sum
    
    if errs.nonEmpty then Left(errs -> jarCount) else Right(jarCount)
  
  def main(path: String): Unit = 
    checkVersionsRecursively(os.Path(path), 55, "codegen", "scripts") match
      case Left((errs, results)) =>
        println(s"Failures [${errs.size}/${results}]:")
        errs.foreach(println)
        sys.exit(1)
      case Right(results) =>
        println(s"All $results jars conform to the binary versioning scheme!")
        sys.exit(0)
end JarVersions