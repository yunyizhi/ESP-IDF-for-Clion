plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"
}
//val buildType = "231"
val buildType = "default"
val version231 = "0.3.231-beta"
val versionDefault = "0.4.2"


group = "org.btik"
version = if (buildType == "231") version231 else versionDefault

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.aliyun.com/repository/public/")
    }
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
                "terminal",
                "sh"
            )
        )
    } else {
        version.set("LATEST-EAP-SNAPSHOT")
        plugins.set(
            listOf(
                "com.intellij.cidr.base",
                "com.intellij.clion",
                "nativeDebug-plugin",
                "com.intellij.clion.embedded",
                "clion-ide",
                "terminal",
                "sh"
            )
        )
    }

}

if (buildType == "231") {
    sourceSets.getByName("main").java.srcDirs("src/adapterPack/source4v231")
} else {
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
            sinceBuild.set("242")
            untilBuild.set("243.*")
        }
        changeNotes.set(
            """
            <h3>0.4</h3>
            en:
            <p>Integrated OpenOCD Debugging Support:</p>
            <ul>
                <li>Breakpoint Debugging</li>
                <li>Thread and Variable Inspection</li>
                <li>GDB Command Execution</li>
                <li>Memory View</li>
                <li>Peripheral Register Viewing</li>
            </ul>
            中文:
            <p>集成openocd调试:</p>
            <ul>
                <li>clion图形化断点调试</li>
                <li>线程和变量查看</li>
                <li>gdb控制台</li>
                <li>内存视图</li>
                <li>外设寄存器视图</li>
            </ul>
            <h3>0.3</h3>
            en:
            <ul>
                <li>
                    Support selecting an `ESP-IDF Target` when creating a project.
                </li>
                <li>
                    Support configuration of serial port number, monitoring, and upload baud rate in the new tab `Settings` within
                    `ESP-IDF Quick Tools`。
                </li>
                <li>
                    Additional commands have been added to the task tree.
                </li>
            </ul>
            中文:
            <ul>
                <li>
                    新建项目:支持创建项目时选择target.
                </li>
                <li>
                    ESP-IDF工具窗:新增`Settings` 页签, 目前支持串口号,监控波特率和上传波特率配置。
                </li>
                <li>
                    任务树:更多的命令封装，新增了更多命令。
                </li>
            </ul>
            <h3>0.2</h3>
            en:
            <p>
                Support for ESP-IDF installation from source on Windows.
            </p>
            <p>
                Added `IDF Export Console` to address the scenario where the `espefuse.py` tool was unavailable in CLion's terminal.
            </p>
            中文:
            <p>
                支持Windows下通过源码安装的ESP-IDF。
            </p>
            <p>
                添加了IDF Export Console 修复espefuse.py 工具在CLion的终端中不可用的场景。
            </p>
            """
        )
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
