plugins {
    `java-library`
}

dependencies {
    api(project(":base"))
    api(project(":common"))
    api(libs.joml)
    compileOnlyApi(libs.jspecify)

    testRuntimeOnly(project(":assets"))
}
