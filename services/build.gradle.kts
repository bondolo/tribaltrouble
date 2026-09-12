plugins {
    `java-library`
    application
}

java {
    modularity.inferModulePath.set(false)
}

application {
    mainClass.set("com.oddlabs.matchservlet.Application")
}

dependencies {
    implementation(project(":common"))
    implementation(platform(libs.micronaut.platform))
    annotationProcessor(platform(libs.micronaut.platform))
    annotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.serde.processor)

    implementation(libs.micronaut.http.server.netty)
    implementation(libs.micronaut.serde.jackson)
    implementation(libs.micronaut.jdbc.hikari)
    implementation(libs.micronaut.flyway)
    implementation(libs.flyway.core)
    implementation(libs.flyway.mysql)
    implementation(libs.h2)
    implementation(libs.mysql.connector)
    implementation(libs.snakeyaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(platform(libs.micronaut.platform))
    testAnnotationProcessor(platform(libs.micronaut.platform))
    testAnnotationProcessor(libs.micronaut.inject.java)
    testAnnotationProcessor(libs.micronaut.serde.processor)
    testImplementation(libs.micronaut.test.junit5)
    testImplementation(libs.micronaut.http.client)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.oddlabs.matchservlet.Application"
    }
    from(sourceSets["main"].output)
    dependsOn(configurations["runtimeClasspath"])
    from({
        configurations["runtimeClasspath"].filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
