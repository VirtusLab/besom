//> using scala "3.3.4"

//> using resourceDir "./src/main/resources"

//> using dep "org.scala-lang::scala3-compiler:3.3.4"

// this is intentional - we need compiler plugin to work with the same range of jvms as the compiler itself
//> using options "-java-output-version:8"

//> using publish.organization "org.virtuslab"
//> using publish.name "yaga-aws-compiler-plugin"
//> using publish.version 0.4.0-SNAPSHOT
//> using publish.url "https://github.com/VirtusLab/besom"
//> using publish.license "Apache-2.0"
//> using publish.repository "central"
//> using publish.developer "lbialy|Łukasz Biały|https://github.com/lbialy"
//> using publish.developer "prolativ|Michał Pałka|https://github.com/prolativ"
