package besom.codegen

//noinspection ScalaFileName
class PulumiTokenTest extends munit.FunSuite {
  test("apply") {
    assertEquals(PulumiToken("aws:ec2:Instance"), PulumiToken("aws", "ec2", "Instance"))
    assertEquals(PulumiToken("pulumi:providers:aws"), PulumiToken("pulumi", "providers", "aws"))
    assertEquals(PulumiToken("pulumi", "providers", "aws"), PulumiToken("pulumi", "providers", "aws"))
  }
  test("asString") {
    assertEquals(PulumiToken("aws:ec2:Instance").asString, "aws:ec2:Instance")
    assertEquals(PulumiToken("example", "index", "SomeType").asString, "example:index:SomeType")
  }
  test("asLookupKey") {
    assertEquals(PulumiToken("aws:ec2:Instance").asLookupKey, "aws:ec2:instance")
  }

  test("enforce non-empty module") {
    assertEquals(PulumiToken("example", "", "SomeType").asString, "example:index:SomeType")
    assertEquals(PulumiToken("example::SomeType").asString, "example:index:SomeType")
    assertEquals(PulumiToken("example:SomeType").asString, "example:index:SomeType")
  }
}
