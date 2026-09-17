import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.nullaway) apply false
    alias(libs.plugins.lwjgl3) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "com.oddlabs.tribaltrouble"
    version = "2.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "com.diffplug.spotless")

    configure<SpotlessExtension> {
        java {
            ratchetFrom("origin/master")
            target("src/**/*.java")
            eclipse().configFile(rootProject.file("intellij-java-style.xml"))
            removeUnusedImports("cleanthat-javaparser-unnecessaryimport")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    dependencies {
        implementation(rootProject.libs.jspecify)
        "errorprone"(rootProject.libs.errorprone.core)
        "errorprone"(rootProject.libs.nullaway)

        testImplementation(platform(rootProject.libs.junit.bom))
        testImplementation(rootProject.libs.junit.jupiter)
        testImplementation(rootProject.libs.junit.jupiter.params)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_27
        targetCompatibility = JavaVersion.VERSION_27
        modularity.inferModulePath.set(true)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.errorprone {
            option("NullAway:AnnotatedPackages", "com.oddlabs")
            disableAllChecks = false


            disable(  "NullAway", "ImmutableEnumChecker",
                "NarrowingCompoundAssignment", "TypeParameterQualifier",
                "SameNameButDifferent", "AssignmentExpression",
                "ObjectToString", "ReferenceEquality",
                "ModifyCollectionInEnhancedForLoop", "StringCaseLocaleUsage",
                "EqualsHashCode", "DoNotCallSuggester",
                "MutablePublicArray", "InconsistentCapitalization",
                "EnumOrdinal", "UnnecessaryParentheses", "UnusedMethod", "UnusedVariable",
                "StatementSwitchToExpressionSwitch",
                "ArrayRecordComponent", "StringSplitter" )
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
