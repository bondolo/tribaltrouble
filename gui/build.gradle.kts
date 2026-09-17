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
    api(project(":window"))
    api(project(":input"))
    api(project(":engine"))
    compileOnlyApi(libs.jspecify)
}
