plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    compileOnlyApi(libs.jspecify)
}
