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
    implementation(project(":base"))
    implementation(project(":common"))
    api(project(":simulation"))
    implementation(project(":procedural"))
    api(project(":engine"))
    implementation(libs.joml)
    compileOnlyApi(libs.jspecify)
}
