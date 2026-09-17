import com.smushytaco.lwjgl_gradle.Module

plugins {
    `java-library`
    alias(libs.plugins.lwjgl3)
}

lwjgl {
    version = libs.versions.lwjgl.get()
    implementation(
        Module.CORE,
        Module.STB
    )
}

dependencies {
    api(project(":base"))
    api(libs.joml)
    compileOnlyApi(libs.jspecify)
}
