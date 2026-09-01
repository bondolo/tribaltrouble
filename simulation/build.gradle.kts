plugins {
    `java-library`
}

dependencies {
    api(project(":base"))
    api(project(":common"))
    api(libs.joml)
    compileOnlyApi(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(project(":assets"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
