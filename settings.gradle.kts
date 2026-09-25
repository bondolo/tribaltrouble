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

// Foundation and platform abstractions
include("common", "base", "window", "input", "audio", "audio-openal")

// Simulation, procedural generation, and networking
include("simulation", "procedural", "net")

// Graphics engine, presentation, and scenery
include("engine", "effects", "gui", "scenery")

// asset pipeline, game client and content
include("client", "content", "assets")

// Headless runtime, dedicated server, and matchmaking services
include("headless", "server", "services")

// Application launcher, and developer tools
include("tt", "tools")
