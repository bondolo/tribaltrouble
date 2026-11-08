plugins {
    `java-library`
}

dependencies {
    api(project(":simulation"))
    api(project(":ai"))
    implementation(project(":base"))
    implementation(project(":common"))
    compileOnlyApi(libs.jspecify)

    testImplementation(testFixtures(project(":simulation")))
    testRuntimeOnly(project(":assets"))
}
