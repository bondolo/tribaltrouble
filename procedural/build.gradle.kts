plugins {
    `java-library`
}

dependencies {
    api(project(":simulation"))
    implementation(project(":base"))
    implementation(project(":common"))
    api(libs.joml)
    compileOnlyApi(libs.jspecify)
}
