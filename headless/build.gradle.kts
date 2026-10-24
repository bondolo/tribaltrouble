plugins {
    `java-library`
    application
}

application {
    mainClass.set("com.oddlabs.tt.headless.Main")
}

dependencies {
    implementation(project(":base"))
    implementation(project(":common"))
    implementation(project(":simulation"))
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
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    systemProperty("com.oddlabs.tt.legacy.deterministic", System.getProperty("com.oddlabs.tt.legacy.deterministic", "true"))
    systemProperty("junit.jupiter.execution.parallel.enabled", System.getProperty("junit.jupiter.execution.parallel.enabled", "true"))
    systemProperty("junit.jupiter.execution.parallel.mode.default", System.getProperty("junit.jupiter.execution.parallel.mode.default", "concurrent"))
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
}
