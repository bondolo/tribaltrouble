plugins {
    `java-library`
}

dependencies {
    compileOnlyApi(libs.jspecify)
    api(libs.joml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.addAll(listOf("--add-modules", "java.desktop"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--add-modules", "java.desktop")
}