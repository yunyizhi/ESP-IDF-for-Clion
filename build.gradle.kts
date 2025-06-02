import java.io.File

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "org.btik"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.aliyun.com/repository/public/")
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    intellijPlatform {
        defaultRepositories()
        marketplace()
    }
}

dependencies {
    intellijPlatform {
        clion("2025.1", useInstaller = false)
        bundledPlugins(
            "com.intellij.cidr.base",
            "com.intellij.clion",
            "com.intellij.clion.embedded",
            "com.jetbrains.sh",
            "com.intellij.nativeDebug"
        )
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        val changeNoteHtml = "change_note.html"
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
sourceSets {
    main {
        resources.srcDirs("${project.projectDir}/web_src/dist")
    }
}

tasks.register<Exec>("npmBuild") {
    group = "build"
    description = "Run npm build for web assets"
    workingDir = file("${project.projectDir}/web_src")

    val profile = project.properties["profile"]?.toString() ?: "dev"

    val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
    commandLine = if (isWindows) {
        listOf("cmd", "/c", "npm", "run", "build", "--", "--mode=$profile")
    } else {
        listOf("bash", "-c", "npm run build -- --mode=$profile")
    }
}

// 2. 将 npmBuild 任务挂载到 processResources 阶段
tasks.named("processResources") {
    dependsOn("npmBuild") // 在资源处理前执行前端构建
}

tasks.register<Delete>("cleanWeb") {
    delete("${project.projectDir}/web_src/dist") // 删除前端输出目录
}

tasks.named("clean") {
    dependsOn("cleanWeb")
}