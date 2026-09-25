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
    implementation(project(":simulation"))
    implementation(project(":procedural"))
    implementation(project(":net"))
    implementation(project(":window"))
    implementation(project(":input"))
    implementation(project(":audio"))
    implementation(project(":engine"))
    implementation(project(":scenery"))
    implementation(project(":gui"))
    implementation(project(":client"))
    compileOnlyApi(libs.jspecify)
}
