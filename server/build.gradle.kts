plugins {
    `java-library`
}

dependencies {
    implementation(project(":common"))
    implementation(libs.h2)
    implementation(libs.mysql.connector)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
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
