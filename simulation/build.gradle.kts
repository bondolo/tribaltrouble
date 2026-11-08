plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":base"))
    api(project(":common"))
    api(libs.joml)
    compileOnlyApi(libs.jspecify)
    testFixturesCompileOnly(libs.jspecify)

    testRuntimeOnly(project(":assets"))
}
