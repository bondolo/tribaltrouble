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
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    dependencies {
        implementation(rootProject.libs.jspecify)
        "errorprone"(rootProject.libs.errorprone.core)
        "errorprone"(rootProject.libs.nullaway)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_26
        targetCompatibility = JavaVersion.VERSION_26
        modularity.inferModulePath.set(true)
    }

    tasks.withType<JavaCompile>().configureEach {
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
}
