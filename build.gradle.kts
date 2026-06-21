import java.io.File

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = "org.btik"

repositories {
    maven {
        url = uri("https://maven.aliyun.com/repository/public/")
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        marketplace()
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.20.0")
    implementation("com.fazecast:jSerialComm:2.11.4")
    intellijPlatform {
        clion("2026.1") { useInstaller = false }
        bundledPlugins(
            "com.intellij.clion",
            "com.intellij.clion.cmake",
            "com.intellij.clion.embedded",
            "com.jetbrains.sh",
            "org.jetbrains.plugins.terminal",
            "com.intellij.nativeDebug"
        )
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        val changeNoteHtml = "./change_note.html"
        val changeNoteFile = File(changeNoteHtml)

        if (changeNoteFile.exists()) {
            val fileContent = changeNoteFile.readText()
            val bodyRegex = Regex("<body>(.*?)</body>", RegexOption.DOT_MATCHES_ALL)
            val bodyContent = bodyRegex.find(fileContent)?.groupValues?.get(1)?.trim() ?: ""
            changeNotes = bodyContent
        } else {
            println("Error: File '$changeNoteHtml' does not exist.")
        }



        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}


tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }
}

tasks.register<Delete>("unzipWebHelpAndPreserveXsd") {
    val docsDir = file("./docs")
    delete(fileTree(docsDir) {
        include("**/*")
        exclude("**/*.xsd")
    })

    finalizedBy("actuallyUnzipWebHelp")
}

tasks.register<Copy>("actuallyUnzipWebHelp") {
    mustRunAfter("unzipWebHelpAndPreserveXsd")

    val zipFile = file("./doc_source/webHelpESPIDF2-all.zip")
    val docsDir = file("./docs")

    from(zipTree(zipFile))
    into(docsDir)
    inputs.file(zipFile)
    outputs.dir(docsDir)

    doFirst {
        logger.lifecycle("Unzipping ${zipFile.name} into ${docsDir.absolutePath}")
    }
}


tasks.register("updateDocs") {
    group = "custom"
    description = "Cleans docs dir (preserving XSDs) and then unpacks the latest web help zip"

    dependsOn("unzipWebHelpAndPreserveXsd", "actuallyUnzipWebHelp")
}

// ---------- Vite Frontend Build ----------
val webSizeDir = layout.projectDirectory.dir("web-size")
val npmCommand = if (System.getProperty("os.name").lowercase().contains("win")) "npm.cmd" else "npm"

tasks.register<Exec>("webSizeInstall") {
    group = "web"
    description = "Run npm install in web-size/"
    workingDir = webSizeDir.asFile
    commandLine(npmCommand, "install")
    inputs.file(webSizeDir.file("package.json"))
    outputs.dir(webSizeDir.dir("node_modules"))
}

tasks.register<Exec>("webSizeBuild") {
    group = "web"
    description = "Build Vue frontend with Vite"
    dependsOn("webSizeInstall")
    workingDir = webSizeDir.asFile
    commandLine(npmCommand, "run", "build")
    inputs.dir(webSizeDir.dir("src"))
    inputs.file(webSizeDir.file("index.html"))
    inputs.file(webSizeDir.file("vite.config.js"))
    outputs.dir(layout.projectDirectory.dir("src/main/resources/web"))
}

tasks.named("processResources") {
    dependsOn("webSizeBuild")
}

tasks.register<Delete>("webSizeClean") {
    group = "web"
    description = "Clean Vite build output"
    delete(layout.projectDirectory.dir("src/main/resources/web"))
}