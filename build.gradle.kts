/*
 * Copyright 2025-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.adarshr.gradle.testlogger.theme.ThemeType

buildscript {
    repositories {
        gradlePluginPortal()
        maven(url = "https://repo.spring.io/plugins-release/")
    }
}

plugins {
    id("eclipse")
    id("idea")
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.spotless)
    alias(libs.plugins.test.logger)
}

description = "Spring Session and Spring MongoDB integration"

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) } // Remember to update javadoc links
    withJavadocJar()
    withSourcesJar()
    registerFeature("optional") { usingSourceSet(sourceSets["main"]) }
}

// Suppress POM warnings for the optional features (eg: optionalApi, optionalImplementation)
// afterEvaluate {
//    configurations
//        .filter { it.name.startsWith("optional") }
//        .forEach { optional ->
//            publishing.publications.named<MavenPublication>("maven") { suppressPomMetadataWarningsFor(optional.name) }
//        }
// }

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Integration Test

// Added `Action` explicitly due to an intellij 2025.2 false positive: https://youtrack.jetbrains.com/issue/KTIJ-34210
sourceSets {
    create(
        "integrationTest",
        Action {
            compileClasspath += sourceSets.main.get().output
            runtimeClasspath += sourceSets.main.get().output
        })
}

val integrationTestSourceSet: SourceSet = sourceSets["integrationTest"]

val integrationTestImplementation: Configuration by
    configurations.getting {
        extendsFrom(
            configurations.getByName("api"),
            configurations.getByName("optionalApi"),
            configurations.getByName("implementation"),
            configurations.getByName("optionalImplementation"),
            configurations.getByName("testImplementation"))
    }
val integrationTestRuntimeOnly: Configuration by
    configurations.getting {
        extendsFrom(configurations.getByName("runtimeOnly"), configurations.getByName("testRuntimeOnly"))
    }

val integrationTestTask =
    tasks.register<Test>("integrationTest") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath
    }

tasks.check { dependsOn(integrationTestTask) }

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // Pass any `org.mongodb.*` system settings
    systemProperties =
        System.getProperties()
            .map { (key, value) -> Pair(key.toString(), value) }
            .filter { it.first.startsWith("org.mongodb.") }
            .toMap()
}

// Pretty test output
testlogger {
    theme = ThemeType.STANDARD
    showExceptions = true
    showStackTraces = true
    showFullStackTraces = false
    showCauses = true
    slowThreshold = 2000
    showSummary = true
    showSimpleNames = false
    showPassed = true
    showSkipped = true
    showFailed = true
    showOnlySlow = false
    showStandardStreams = false
    showPassedStandardStreams = true
    showSkippedStandardStreams = true
    showFailedStandardStreams = true
    logLevel = LogLevel.LIFECYCLE
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Dependencies

dependencies {
    api(platform(libs.spring.session.bom))
    api(libs.spring.session.core)

    api(platform(libs.spring.data.bom))
    api(libs.spring.data.mongodb)

    api(platform(libs.jackson.bom))
    api(libs.jackson.databind)

    api(platform(libs.spring.security.bom))
    api(libs.spring.security.core)

    // Optional dependencies
    "optionalApi"(platform(libs.project.reactor.bom))
    "optionalApi"(libs.project.reactor.core)

    implementation(platform(libs.mongodb.driver.bom))
    implementation(libs.mongodb.driver.core)
    "optionalImplementation"(libs.mongodb.driver.sync)
    "optionalImplementation"(libs.mongodb.driver.reactive.streams)

    // We need the `libs.findbugs.jsr` dependency to stop `javadoc` from emitting
    // `warning: unknown enum constant When.MAYBE`
    //   `reason: class file for javax.annotation.meta.When not found`.
    compileOnly(libs.findbugs.jsr)

    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.mockito.bom))
    testImplementation(platform(libs.spring.framework.bom))

    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.jakarta)
    testImplementation(libs.bundles.spring.test)
    testImplementation(libs.logback.core)
    testImplementation(libs.project.reactor.test)

    testRuntimeOnly(libs.junit.platform.launcher)
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Static Analysis

tasks.named("check") { dependsOn("spotlessApply") }

spotless {
    java {
        importOrder()

        removeUnusedImports()

        palantirJavaFormat(libs.versions.plugin.palantir.get()).formatJavadoc(true)

        formatAnnotations()

        // need to add license header manually to package-info.java and module-info.java
        // due to the bug: https://github.com/diffplug/spotless/issues/532
        licenseHeaderFile(file("config/spotless.license.java"))

        targetExclude("build/generated/sources/buildConfig/**/*.java")
    }

    kotlinGradle {
        ktfmt(libs.versions.plugin.ktfmt.get()).configure {
            it.setMaxWidth(120)
            it.setBlockIndent(4)
        }
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }

    format("extraneous") {
        target("*.xml", "*.yml", "*.md", "*.toml")
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
}
