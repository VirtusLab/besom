//> using target.platform jvm

//> using dep com.amazonaws:aws-lambda-java-core:1.2.3
// Adding a version of software.amazon.awssdk:lambda newer than 2.26.9 (at least until 2.28.26) to the classpath magically causes besom.internal.ResourceDecoder.resolve to crash at runtime
//> using dep software.amazon.awssdk:lambda:2.26.9