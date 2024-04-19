plugins {
    scala
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.3.1")
    implementation("org.virtuslab:besom-core_3:0.3.1")
    implementation("org.virtuslab:besom-fake-standard-resource_3:1.2.3-TEST")
    implementation("org.virtuslab:besom-fake-external-resource_3:2.3.4-TEST")
    if (project.hasProperty("besomBootstrapJar")) runtimeOnly(files(project.property("besomBootstrapJar") as String))
}

application {
    mainClass.set(
            if (project.hasProperty("mainClass")) {
                project.property("mainClass") as String
            } else {
                "besom.languageplugin.test.pulumiapp.run"
            }
    )
}
