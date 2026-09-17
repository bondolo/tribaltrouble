import com.smushytaco.lwjgl_gradle.Module

plugins {
    `java-library`
    alias(libs.plugins.lwjgl3)
}

lwjgl {
    // Strongly recommended: set LWJGL version explicitly
    version = libs.versions.lwjgl.get()

    // Add LWJGL modules + the correct native artifacts
    implementation(
        Module.CORE,
        Module.OPENGL,
        Module.STB)
}

dependencies {
    api(project(":common"))
}
