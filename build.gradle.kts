import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.errorprone

import java.io.File

plugins {
    java
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.nullaway) apply false
    alias(libs.plugins.lwjgl3) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "com.oddlabs.tribaltrouble"
    version = "2.0"
}

// Validation posture toggle: -PlwjglxDebug attaches the lwjglx-debug agent to every JavaExec + Test fork in every module
// and routes the also-soft log and lwjglx trace log to absolute per-module log dirs that scanLogs / scanAudioLogs read
// afterwards.
val lwjglxDebug = (findProperty("lwjglxDebug") as? String)?.toBoolean() == true

// Headless-driver toggle, off by default: -PalsoSoftNullDriver also sets ALSOFT_DRIVERS=null so OpenAL-Soft selects its
// null output driver for a CI host with no audio hardware. On this build (macOS, OpenAL-Soft 1.25.2) the null driver is
// NOT a no-op: alcOpenDevice still succeeds yet alDeleteSources then aborts the JVM natively (SIGABRT in libopenal.dylib),
// so it is opt-in and should only be used on a platform where also-soft's null driver is clean (e.g. a Linux CI image).
val alsoSoftNullDriver = (findProperty("alsoSoftNullDriver") as? String)?.toBoolean() == true

// Agent jar path is resolved from the local Maven repository so the build is not tied to one machine's home path; override
// for CI via -PlwjglxDebugJar=/path/to/jar or the version via -PlwjglxDebugVersion. 1.1.0 (bondolo/debug `validation`
// branch, on lwjgl.org/addons) is the first release that adds OpenAL lifecycle tracking (use-after-free, double-free, and
// teardown-leak auditing) on top of GL/AL argument validation; 1.0.6 covers GL+SDL arg validation only and has no OpenAL
// classes, so 1.1.0 is the default.
val lwjglxDebugVersion = (findProperty("lwjglxDebugVersion") as? String) ?: "1.1.0"
val lwjglxDebugJarPath =
        findProperty("lwjglxDebugJar") as? String
                   ?: System.getProperty("user.home") + "/.m2/repository/org/lwjglx/lwjglx-debug/" +
                            lwjglxDebugVersion + "/lwjglx-debug-" + lwjglxDebugVersion + ".jar"

// Per-module log dir lives under the module's own build directory so modules that run in parallel don't race on the same
// files. The root-level scanAudioLogs aggregator walks every module's build/debug/lwjglx/ directory.
fun Project.lwjglxDebugLogDir(): File = layout.buildDirectory.dir("debug/lwjglx").get().asFile

// Also-soft renderer-side error lines (written to openal.log via ALSOFT_LOGFILE) and lwjglx agent argument / trace failures.
val openalMarkers = listOf("also-soft error" to Regex("(?i)(alsoft.*error|unsupported .* by OpenAL-Soft)"))
val lwjglxMarkers = listOf(
        "lwjglx argument" to Regex("must not be null|produced error|GL_INVALID_\\w+|AL_INVALID_\\w+|before initializing"),
        "lwjglx trace" to Regex("^\\[error\\]"))

// Scans every *.log in a module's debug dir and, if any known failure marker is found, writes a summary and throws so CI
// fails; a missing dir or file is treated as "no matches" rather than an error.
fun scanLogDir(logDir: File, hits: MutableList<String>) {
    if (!logDir.exists()) return
    for (logFile in logDir.listFiles { _, name -> name.endsWith(".log") } ?: emptyArray()) {
        var line = 0
        for (raw in logFile.readLines()) {
            line++
            for ((label, regex) in openalMarkers + lwjglxMarkers) {
                if (regex.containsMatchIn(raw)) hits.add("[$label] ${logFile.name}:$line: ${raw.trim()}")
            }
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "com.diffplug.spotless")

    configure<SpotlessExtension> {
        java {
            ratchetFrom("origin/master")
            target("src/**/*.java")
            eclipse().configFile(rootProject.file("intellij-java-style.xml"))
            removeUnusedImports("cleanthat-javaparser-unnecessaryimport")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    dependencies {
        implementation(rootProject.libs.jspecify)
        "errorprone"(rootProject.libs.errorprone.core)
        "errorprone"(rootProject.libs.nullaway)

        testImplementation(platform(rootProject.libs.junit.bom))
        testImplementation(rootProject.libs.junit.jupiter)
        testImplementation(rootProject.libs.junit.jupiter.params)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_27
        targetCompatibility = JavaVersion.VERSION_27
        modularity.inferModulePath.set(true)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.errorprone {
            option("NullAway:AnnotatedPackages", "com.oddlabs")
            disableAllChecks = false

            disable(  "NullAway", "ImmutableEnumChecker",
                    "NarrowingCompoundAssignment", "TypeParameterQualifier",
                    "SameNameButDifferent", "AssignmentExpression",
                    "ObjectToString", "ReferenceEquality",
                    "ModifyCollectionInEnhancedForLoop", "StringCaseLocaleUsage",
                    "EqualsHashCode", "DoNotCallSuggester",
                    "MutablePublicArray", "InconsistentCapitalization",
                    "EnumOrdinal", "UnnecessaryParentheses", "UnusedMethod", "UnusedVariable",
                    "StatementSwitchToExpressionSwitch",
                    "ArrayRecordComponent", "StringSplitter")
        }
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }

    // When -PlwjglxDebug is on, attach the lwjglx agent to every JavaExec + Test fork in this module and route the
    // also-soft log to an absolute per-module path. The agent writes its [error] trace to trace.log and OpenAL-Soft
    // writes renderer issues to openal.log, so no stdout redirection is needed; scanLogs / scanAudioLogs read those
    // files after the run. ALSOFT_DRIVERS=null is gated separately behind -PalsoSoftNullDriver because it aborts this
    // platform's OpenAL-Soft natively on teardown (see the flag above).
   if (lwjglxDebug) {
       val agentJar = File(lwjglxDebugJarPath)
       val agentArg = "-javaagent:${agentJar.absolutePath}=validate;strict;trace;nothrow;" +
                "output=${lwjglxDebugLogDir().resolve("trace.log").absolutePath}"
       if (!agentJar.exists()) {
            logger.lifecycle("lwjglxDebug enabled but agent jar not found at $agentJar — tasks will NOT be instrumented. " +
                    "Place lwjglx-debug-$lwjglxDebugVersion.jar at $agentJar " +
                    "(https://www.lwjgl.org/addons/lwjglx-debug) or override via -PlwjglxDebugJar=/path/to/jar.")
        }

    // JavaExec (e.g. :tt:run, :headless:run) forks the full application.
        tasks.withType<JavaExec>().configureEach {
            if (agentJar.exists()) jvmArgs(agentArg)
            if (alsoSoftNullDriver) environment("ALSOFT_DRIVERS", "null")
             environment("ALSOFT_LOGLEVEL", "3")
             environment("ALSOFT_LOGFILE", lwjglxDebugLogDir().resolve("openal.log").absolutePath)
         }

          // Test forks (e.g. :audio-openal:test integration fixtures): attach the agent so fixtures that exercise OpenAL
          // lifecycle paths are validated; on a headless CI, pair with -PalsoSoftNullDriver to pick a device-less driver.
        tasks.withType<Test>().configureEach {
            if (agentJar.exists()) jvmArgs(agentArg)
            if (alsoSoftNullDriver) environment("ALSOFT_DRIVERS", "null")
             environment("ALSOFT_LOGLEVEL", "3")
             environment("ALSOFT_LOGFILE", lwjglxDebugLogDir().resolve("openal.log").absolutePath)
         }
     }

    // Per-module inspection: scans this module's build/debug/lwjglx/*.log and fails on any known marker.
    tasks.register("scanLogs") {
        doFirst {
            val hits = mutableListOf<String>()
            scanLogDir(lwjglxDebugLogDir(), hits)
            if (hits.isEmpty()) {
                logger.lifecycle("scanLogs (${project.path}): no failure markers.")
            } else {
                val summary = "scanLogs (${project.path}): FAIL — ${hits.size} failure marker(s)" +
                        "\n" + hits.joinToString("\n") { "   $it" }
                lwjglxDebugLogDir().resolve("scanLogs.summary.txt").writeText(summary)
                throw GradleException(summary)
            }
        }
    }
}

// Root aggregator: scan every module's captured logs in one shot so CI can run a single :scanAudioLogs entry point.
// dependsOn("scanLogs") resolves to every task named "scanLogs" across all subprojects at execution time.
tasks.register("scanAudioLogs") {
    dependsOn("scanLogs")
}
