plugins {
    `java-library`
    war
}

java {
    modularity.inferModulePath.set(false)
}

dependencies {
    implementation(project(":common"))
    implementation(libs.servlet.api)
    implementation(libs.h2)
}

tasks.register<War>("matchservlet") {
    dependsOn("classes")
    archiveFileName.set("matchservlet.war")
    webXml = file("descriptors/matchservlet/web.xml")
    from(sourceSets["main"].output) {
        include("com/oddlabs/matchservlet/**/*class")
        into("WEB-INF/classes")
    }
    from(project(":common").sourceSets["main"].output) {
        include("com/oddlabs/util/CryptUtils.class")
        into("WEB-INF/classes")
    }
    metaInf {
        from("descriptors/matchservlet") {
            include("context.xml")
        }
    }
}

tasks.register<War>("graphservlet") {
    dependsOn("classes")
    archiveFileName.set("graph.war")
    webXml = file("graphservlet/web.xml")
    from(sourceSets["main"].output) {
        include("com/oddlabs/graphservlet/**/*class")
        into("WEB-INF/classes")
    }
    metaInf {
        from("graphservlet") {
            include("context.xml")
        }
    }
}
