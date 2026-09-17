import com.smushytaco.lwjgl_gradle.Module

plugins {
    `java-library`
    alias(libs.plugins.lwjgl3)
}

lwjgl {
    version = libs.versions.lwjgl.get()
    implementation(
        Module.CORE,
        Module.OPENAL,
        Module.STB
    )
}

dependencies {
    implementation(project(":audio"))
    implementation(libs.joml)
    compileOnlyApi(libs.jspecify)
}
