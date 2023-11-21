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
    assertEquals(PulumiToken("provider", "index", "SomeType").asString, "provider:index:SomeType")
  }
  test("uniformed") {
    assertEquals(PulumiToken("aws:ec2:Instance").uniformed, "aws:ec2:instance")
  }

  test("enforce non-empty module") {
    assertEquals(PulumiToken("provider", "", "SomeType").asString, "provider:index:SomeType")
    assertEquals(PulumiToken("provider::SomeType").asString, "provider:index:SomeType")
    assertEquals(PulumiToken("provider:SomeType").asString, "provider:index:SomeType")
  }
}
