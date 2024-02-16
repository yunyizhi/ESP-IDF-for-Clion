plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"
}
//val buildType = "231"
val buildType = "default"
val version231 = "0.1.231-beta"
val versionDefault = "0.1-beta"


group = "org.btik"
version = if (buildType == "231")  version231 else versionDefault

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    type.set("CL") // Target IDE Platform
    if (buildType == "231") {
        version.set("2023.1.5")
        plugins.set(
            listOf(
                "com.intellij.cidr.base",
                "com.intellij.clion",
                "nativeDebug-plugin",
                "com.intellij.clion.embedded",
                "terminal"
            )
        )
    } else {
        version.set("2023.2.2")
        plugins.set(
            listOf(
                "com.intellij.cidr.base",
                "com.intellij.clion",
                "nativeDebug-plugin",
                "com.intellij.clion.embedded",
                "clion-ide",
                "terminal"
            )
        )
    }

}

if (buildType == "231") {
    sourceSets.getByName("main").java.srcDirs("src/adapterPack/source4v231")
}else {
    sourceSets.getByName("main").java.srcDirs("src/adapterPack/main")
}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        if (buildType == "231") {
            sinceBuild.set("231")
            untilBuild.set("231.*")
        } else {
            sinceBuild.set("232")
            untilBuild.set("241.*")
        }
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
