import com.smushytaco.lwjgl_gradle.Module

plugins {
    application
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    // Strongly recommended: set LWJGL version explicitly
    version = "3.4.2"

    // Add LWJGL modules + the correct native artifacts
    implementation(
        Module.CORE,
        Module.OPENAL,
        Module.OPENGL,
        Module.SDL,
        Module.STB)
}

application {
    mainClass.set("com.oddlabs.tt.Main")
    val args = mutableListOf(
        "-ea", "-esa", //"-check:JNI",
        "--enable-native-access=ALL-UNNAMED",
        "-Djava.awt.headless=true",
        "-Dorg.lwjgl.util.Debug=true",
        "-Dorg.lwjgl.util.DebugLoader=true",
        "-Dcom.oddlabs.tt.developer=true",
        "-Xms80m", "-Xmx512m"
//        , "-javaagent:/Users/mike/.m2/repository/org/lwjglx/lwjglx-debug/1.0.6/lwjglx-debug-1.0.6.jar=validate;trace;output=trace.log"
    )
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        args.add("-XstartOnFirstThread")
    }
    applicationDefaultJvmArgs = args
}

dependencies {
    implementation(project(":common"))
    implementation(project(":assets"))
}

val revision = tasks.register("revision") {
    val output = layout.buildDirectory.file("revision_number")
    outputs.file(output)
    outputs.upToDateWhen { false }
    doLast {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText("gradle-build")
        }
    }
}

tasks.processResources {
    inputs.files(revision)
}

tasks.run.configure {
    classpath = files(layout.buildDirectory) + sourceSets.main.get().runtimeClasspath
}