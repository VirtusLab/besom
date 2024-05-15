package besom.test

import besom.FullyQualifiedStackName

/** SHA1 hash of a string
  *
  * @param s
  *   string to hash
  * @return
  *   40 character hex string of the hash for the given string
  */
def sha1(s: String): String = {
  import java.security.MessageDigest
  val bytes = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
  String.format("%x", new java.math.BigInteger(1, bytes))
}

def sanitizeName(name: String, replacement: String = "-", limit: Int = 40): String =
  name.replaceAll("[^a-zA-Z0-9]+", replacement).toLowerCase().take(limit).stripSuffix(replacement)
def stackName(name: String): String = "tests-" + sanitizeName(name)
def fqsn(`class`: Class[_], test: munit.TestOptions): FullyQualifiedStackName =
  FullyQualifiedStackName(sanitizeName(`class`.getSimpleName, limit = 20), stackName(test.name))
