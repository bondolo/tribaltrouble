rootProject.name = "tribaltrouble"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("assets", "base", "common")
// server and servlet excluded - have compilation errors
// include("server", "servlet")
include("audio", "audio-openal", "client", "content", "effects", "engine", "gui", "input", "net", "procedural", "simulation", "tools", "tt", "window")
