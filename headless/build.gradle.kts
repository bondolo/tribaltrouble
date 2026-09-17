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
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
}
