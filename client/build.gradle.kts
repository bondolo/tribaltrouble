import com.smushytaco.lwjgl_gradle.Module

plugins {
    `java-library`
    alias(libs.plugins.lwjgl3)
}

lwjgl {
    version = libs.versions.lwjgl.get()
    implementation(
        Module.CORE,
        Module.OPENGL
    )
}

dependencies {
    api(project(":base"))
    api(project(":common"))
    api(project(":simulation"))
    api(project(":procedural"))
    api(project(":net"))
    api(project(":window"))
    api(project(":audio"))
    api(project(":engine"))
    implementation(project(":effects"))
    implementation(project(":gui"))
    compileOnlyApi(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
