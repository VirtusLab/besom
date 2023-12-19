package besom.model

import besom.test.CompileAssertions

class NamesTest extends munit.FunSuite with CompileAssertions:

  import QName.* // force use of QName extensions instead of TestOptionsConversions.name

  test("validation - all alpha") {
    compiles("""import besom.model._
             val n = Name("simple")
             """)
  }

  test("validation - mixed-case alpha") {
    compiles("""import besom.model._
                val n = Name("SiMplE")
                """)
  }

  test("validation - alphanumeric") {
    compiles("""import besom.model._
                val n = Name("simple0")
                """)
  }

  test("validation - mixed-case alphanumeric") {
    compiles("""import besom.model._
                val n = Name("SiMpLe0")
                """)
  }

  test("validation - permit underscore") {
    compiles("""import besom.model._
                val n = Name("_")
                """)
  }

  test("validation - mixed-case alphanumeric/underscore") {
    compiles("""import besom.model._
                val n = Name("s1MPl3_")
                """)
    compiles("""import besom.model._
                val n = Name("_s1MPl3")
                """)
  }

  test("validation - permit hyphens") {
    compiles("""import besom.model._
                val n = Name("hy-phy")
                """)
  }

  test("validation - start with .") {
    compiles("""import besom.model._
                val n = Name(".dotstart")
                """)
  }

  test("validation - start with -") {
    compiles("""import besom.model._
                val n = Name("-hyphenstart")
                """)
  }

  test("validation - start with numbers") {
    compiles("""import besom.model._
                val n = Name("0num")
                """)
  }

  test("validation - start with numbers") {
    compiles("""import besom.model._
                val n = Name("9num")
                """)
  }

  test("validation - multi-part name") {
    compiles("""import besom.model._
                val n = QName("namespace/complex")
                """)
    failsToCompile("""import besom.model._
                      val n = Name("namespace/complex")
                      """)
  }

  test("validation - multi-part, alphanumeric, etc. name") {
    compiles("""import besom.model._
                val n = QName("_naMeSpace0/coMpl3x32")
                """)
    failsToCompile("""import besom.model._
                      val n = Name("_naMeSpace0/coMpl3x32")
                      """)
  }

  test("validation - even more complex parts") {
    compiles("""import besom.model._
                val n = QName("n_ameSpace3/moRenam3sp4ce/_Complex5")
                """)
    failsToCompile("""import besom.model._
                      val n = Name("n_ameSpace3/moRenam3sp4ce/_Complex5")
                      """)
  }

  test("validation - bad characters") {
    failsToCompile("""import besom.model._
                      val n = QName("s!mple")
                      """)
    failsToCompile("""import besom.model._
                      val n = Name("s!mple")
                      """)
    failsToCompile("""import besom.model._
                      val n = QName("namesp@ce/complex")
                      """)
    failsToCompile("""import besom.model._
                      val n = Name("namesp@ce/complex")
                      """)
    failsToCompile("""import besom.model._
                      val n = QName("namespace/morenamespace/compl#x")
                      """)
    failsToCompile("""import besom.model._
                      val n = Name("namespace/morenamespace/compl#x")
                      """)
  }

  test("parsing - simple name") {
    assertEquals(Name("simple"), "simple")
    assertEquals(QName("namespace/complex").name, "complex")
    assertEquals(QName("ns1/ns2/ns3/ns4/complex").name, "complex")
    assertEquals(QName("_/_/_/_/a0/c0Mpl3x_").name, "c0Mpl3x_")
  }

  test("parsing - simple namespace") {
    assertEquals(QName("namespace/complex").namespace, "namespace")
    assertEquals(QName("ns1/ns2/ns3/ns4/complex").namespace, "ns1/ns2/ns3/ns4")
    assertEquals(QName("_/_/_/_/a0/c0Mpl3x_").namespace, "_/_/_/_/a0")
  }

  test("convert to QName") {
    assertEquals(QName.parse("foo/bar"), "foo/bar")
    assertEquals(QName.parse("https:"), "https_")
    assertEquals(QName.parse("https://"), "https_")
    assertEquals(QName.parse(""), "_")
    assertEquals(QName.parse("///"), "_")
  }
end NamesTest
