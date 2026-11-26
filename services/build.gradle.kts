import java.util.zip.ZipFile

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
    implementation(libs.sqlite)
    implementation(libs.snakeyaml)

    testImplementation(platform(libs.micronaut.platform))
    testAnnotationProcessor(platform(libs.micronaut.platform))
    testAnnotationProcessor(libs.micronaut.inject.java)
    testAnnotationProcessor(libs.micronaut.serde.processor)
    testImplementation(libs.micronaut.test.junit5)
    testImplementation(libs.micronaut.http.client)
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.oddlabs.matchservlet.Application"
    }
    val mergedServicesDir = layout.buildDirectory.dir("tmp/fatJar/META-INF/services")
    dependsOn(configurations["runtimeClasspath"])
    doFirst {
        val outDir = mergedServicesDir.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()
        val services = linkedMapOf<String, MutableSet<String>>()
        val sources =
            sourceSets["main"].output.files +
                configurations["runtimeClasspath"].filter { it.name.endsWith(".jar") }
        for (source in sources) {
            if (source.isDirectory) {
                val dir = source.resolve("META-INF/services")
                if (dir.isDirectory) {
                    dir.listFiles()?.filter { it.isFile }?.forEach { file ->
                        services.getOrPut(file.name) { linkedSetOf() }
                            .addAll(
                                file.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
                            )
                    }
                }
            } else if (source.isFile && source.name.endsWith(".jar")) {
                ZipFile(source).use { zip ->
                    zip.entries().asSequence()
                        .filter { !it.isDirectory && it.name.startsWith("META-INF/services/") }
                        .forEach { entry ->
                            val serviceName = entry.name.removePrefix("META-INF/services/")
                            zip.getInputStream(entry).bufferedReader().useLines { lines ->
                                services.getOrPut(serviceName) { linkedSetOf() }
                                    .addAll(lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") })
                            }
                        }
                }
            }
        }
        services.forEach { (serviceName, providers) ->
            outDir.resolve(serviceName).writeText(providers.joinToString("\n", postfix = "\n"))
        }
    }
    from(sourceSets["main"].output) {
        exclude("META-INF/services/**")
    }
    from({
        configurations["runtimeClasspath"].filter { it.name.endsWith("jar") }.map { zipTree(it) }
    }) {
        exclude("META-INF/services/**", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    from(mergedServicesDir) {
        into("META-INF/services")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
