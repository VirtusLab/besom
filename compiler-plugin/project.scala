//> using scala "3.3.0"

//> using publish.organization "org.virtuslab"
//> using publish.name "besom-compiler-plugin"
//> using publish.version "0.0.1-SNAPSHOT"

//> using resourceDir "./src/main/resources"

//> using lib "org.scala-lang::scala3-compiler:3.3.0"

// this is intentional - we need compiler plugin to work with the same range of jvms as the compiler itself
//> using options "-java-output-version:8"
