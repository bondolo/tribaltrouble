plugins {
    `java-library`
}

dependencies {
    api(project(":simulation"))
    implementation(project(":base"))
    implementation(project(":common"))
    compileOnlyApi(libs.jspecify)

    testRuntimeOnly(project(":assets"))
}
