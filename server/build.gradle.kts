plugins {
    `java-library`
}

dependencies {
    implementation(project(":common"))
    implementation(libs.sqlite)
    implementation(libs.flyway.core)
}

sourceSets {
    main {
        resources {
            srcDir(project(":services").file("src/main/resources"))
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.addAll(listOf("--add-reads", "com.oddlabs.server=ALL-UNNAMED"))
}

tasks.register<Jar>("router") {
    dependsOn("classes")
    archiveFileName.set("router.jar")
    from(project(":common").sourceSets["main"].output) {
        include("com/oddlabs/net/**/*class")
        include("com/oddlabs/util/**/*class")
        include("com/oddlabs/event/**/*class")
        include("com/oddlabs/router/**/*class")
    }
    from(sourceSets["main"].output) {
        include("com/oddlabs/routerserver/**/*class")
    }
}

tasks.register<Jar>("matchmaker") {
    dependsOn("classes")
    archiveFileName.set("matchmaking.jar")
    from(project(":common").sourceSets["main"].output) {
        include("com/oddlabs/net/**/*class")
        include("com/oddlabs/util/**/*class")
        include("com/oddlabs/event/**/*class")
        include("com/oddlabs/matchmaking/**/*class")
        include("com/oddlabs/registration/**/*class")
    }
    from(sourceSets["main"].output) {
        include("com/oddlabs/matchserver/**/*class")
    }
}

tasks.register("all") {
    dependsOn("router", "matchmaker")
}
