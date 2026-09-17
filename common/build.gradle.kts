plugins {
    `java-library`
}

dependencies {
    compileOnlyApi(libs.jspecify)
    api(libs.joml)
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.addAll(listOf("--add-modules", "java.desktop"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-modules", "java.desktop")
}