import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("net.ltgt.errorprone") version "5.1.0" apply false
    id("net.ltgt.nullaway") version "3.0.0" apply false
    id("com.smushytaco.lwjgl3") version "1.0.2" apply false
}

allprojects {
    group = "com.oddlabs.tribaltrouble"
    version = "2.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "net.ltgt.errorprone")

    dependencies {
        implementation("org.jspecify:jspecify:1.0.0")
        "errorprone"("com.google.errorprone:error_prone_core:2.49.0")
        "errorprone"("com.uber.nullaway:nullaway:0.13.4")
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
                "NarrowingCompoundAssignment",
                "SameNameButDifferent", "AssignmentExpression",
                "ObjectToString",
                "ModifyCollectionInEnhancedForLoop", "StringCaseLocaleUsage",
                "EqualsHashCode", "DoNotCallSuggester",
                "MutablePublicArray", "InconsistentCapitalization",
                "EnumOrdinal", "UnnecessaryParentheses", "UnusedMethod", "UnusedVariable",
                "StatementSwitchToExpressionSwitch",
                "ArrayRecordComponent", "StringSplitter" )
        }
    }
}
