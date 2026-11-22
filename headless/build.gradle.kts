plugins {
    `java-library`
    application
}

application {
    mainClass.set("com.oddlabs.tt.headless.Main")
    applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true", "-Dapple.awt.UIElement=true")
}

dependencies {
    implementation(project(":base"))
    implementation(project(":common"))
    implementation(project(":simulation"))
    implementation(project(":ai"))
    implementation(project(":procedural"))
    implementation(project(":net"))
    compileOnlyApi(libs.jspecify)

    runtimeOnly(project(":assets"))
    testRuntimeOnly(project(":assets"))
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
    systemProperty("java.awt.headless", "true")
    systemProperty("apple.awt.UIElement", "true")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
    systemProperty("com.oddlabs.tt.legacy.deterministic", System.getProperty("com.oddlabs.tt.legacy.deterministic", "true"))
    systemProperty("junit.jupiter.execution.parallel.enabled", System.getProperty("junit.jupiter.execution.parallel.enabled", "true"))
    systemProperty("junit.jupiter.execution.parallel.mode.default", System.getProperty("junit.jupiter.execution.parallel.mode.default", "concurrent"))
    systemProperty("java.awt.headless", "true")
    systemProperty("apple.awt.UIElement", "true")
}

tasks.register<JavaExec>("regenerateLegacyLibrary") {
    description = "Regenerates legacy_validation_library.csv using LegacyValidationLibraryGenerator."
    group = "verification"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.oddlabs.tt.headless.LegacyValidationLibraryGenerator")
    workingDir = rootProject.projectDir
    jvmArgs(
        "--add-modules",
        "jdk.incubator.vector",
        "-Dcom.oddlabs.tt.legacy.deterministic=true",
        "-Djava.awt.headless=true",
        "-Dapple.awt.UIElement=true")
}
