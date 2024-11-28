//> using scala 3.3.3

//> using dep org.virtuslab::yaga-model:0.4.0-SNAPSHOT
//> using dep com.amazonaws:aws-lambda-java-core:1.2.3
// Adding a version of software.amazon.awssdk:lambda newer than 2.26.9 (at least until 2.28.26) to the classpath magically causes besom.internal.ResourceDecoder.resolve to crash at runtime
//> using dep software.amazon.awssdk:lambda:2.26.9

//> using publish.organization "org.virtuslab"
//> using publish.name "yaga-aws"
//> using publish.version "0.4.0-SNAPSHOT"

//> using publish.license "Apache-2.0"
//> using publish.developer "lbialy|Łukasz Biały|https://github.com/lbialy"
//> using publish.developer "prolativ|Michał Pałka|https://github.com/prolativ"
