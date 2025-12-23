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
import java.time.Duration
import net.ltgt.gradle.errorprone.errorprone

buildscript {
    repositories {
        gradlePluginPortal()
        maven(url = "https://repo.spring.io/plugins-release/")
    }
}

plugins {
    id("eclipse")
    id("idea")
    id("checkstyle")
    id("java-library")
    id("maven-publish")
    id("signing")
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.spotless)
    alias(libs.plugins.test.logger)
    alias(libs.plugins.errorprone)
}

group = "org.mongodb"
description = "Spring Session and Spring MongoDB integration"

val testLatest: Boolean = System.getProperty("latest", "false").toBoolean()

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

tasks.withType<Javadoc> {
    val standardDocletOptions = options as StandardJavadocDocletOptions
    standardDocletOptions.apply {
        author(true)
        version(true)
        encoding = "UTF-8"
        charSet("UTF-8")
        docEncoding("UTF-8")
        addBooleanOption("html5", true)
        addBooleanOption("-allow-script-in-comments", true)
        links =
            listOf(
                "https://docs.oracle.com/en/java/javase/17/docs/api/",
                "https://docs.spring.io/spring-session/docs/3.5.3/api/",
                "https://docs.spring.io/spring-data/mongodb/docs/4.5.5/api/",
                "https://docs.spring.io/spring-security/reference/6.5/api/java/")
    }
}

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
// Build Config

buildConfig {
    className("MongoSpringSessionVersion")
    useJavaOutput()
    packageName("org.mongodb.spring.session.internal")
    buildConfigField("NAME", provider { project.name })
    buildConfigField("VERSION", provider { "${project.version}" })
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Dependencies

dependencies {
    // Check if running a canary check against the latest minor version
    if (testLatest) {
        api(platform(libs.spring.session.bom.latest))
        api(platform(libs.spring.security.bom.latest))
        api(platform(libs.spring.data.bom.latest))
        api(libs.bundles.spring.main.latest)
    } else {
        api(platform(libs.spring.session.bom))
        api(platform(libs.spring.security.bom))
        api(platform(libs.spring.data.bom))
        api(libs.bundles.spring.main)
    }

    api(libs.jspecify)

    // Optional dependencies
    "optionalApi"(platform(libs.jackson2.bom))
    "optionalApi"(libs.jackson2.databind)

    "optionalApi"(platform(libs.jackson3.bom))
    "optionalApi"(libs.jackson3.databind)

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
    errorprone(libs.nullaway)
    errorprone(libs.google.errorprone.core)

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

// Configure errorprone
tasks.withType<JavaCompile>().configureEach {
    if (name.endsWith("TestJava")) {
        options.errorprone.isEnabled = false
    } else {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        options.errorprone {
            disableWarningsInGeneratedCode = true
            option("NullAway:AnnotatedPackages", "org.mongodb.spring.session")
            error("NullAway")
        }
    }
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Publishing

val localBuildRepo: Provider<Directory> = project.layout.buildDirectory.dir("repo")

tasks.named<Delete>("clean") { delete.add(localBuildRepo) }

publishing {
    repositories {
        // publish to local build dir for testing
        // `./gradlew publishMavenPublicationToLocalBuildRepository`
        maven {
            url = uri(localBuildRepo.get())
            name = "LocalBuild"
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "MongoDB Spring Session extension"
                description = "An extension providing MongoDB support for Spring Session"
                url = "https://www.mongodb.com/"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name.set("Various")
                        organization.set("MongoDB")
                    }
                }
                scm {
                    url.set("https://github.com/mongodb/mongo-spring-session")
                    connection.set("scm:https://github.com/mongodb/mongo-spring-session.git")
                    developerConnection.set("scm:https://github.com/mongodb/mongo-spring-session.git")
                }
            }
        }
    }
}

// Suppress POM warnings for the optional features (eg: optionalApi, optionalImplementation)
afterEvaluate {
    configurations
        .filter { it.name.startsWith("optional") }
        .forEach { optional ->
            publishing.publications.named<MavenPublication>("mavenJava") {
                suppressPomMetadataWarningsFor(optional.name)
            }
        }
}

// Artifact signing
signing {
    val signingKey: String? = providers.gradleProperty("signingKey").getOrNull()
    val signingPassword: String? = providers.gradleProperty("signingPassword").getOrNull()
    if (signingKey != null && signingPassword != null) {
        logger.info("[${project.displayName}] Signing is enabled")
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    } else {
        logger.info("[${project.displayName}] No Signing keys found, skipping signing configuration")
    }
}

// Publishing to the central sonatype portal currently requires the gradle nexus publishing plugin
// Adds a `publishToSonatype` task
val nexusUsername: Provider<String> = providers.gradleProperty("nexusUsername")
val nexusPassword: Provider<String> = providers.gradleProperty("nexusPassword")

nexusPublishing {
    packageGroup.set("org.mongodb")
    repositories {
        sonatype {
            username.set(nexusUsername)
            password.set(nexusPassword)

            // central portal URLs
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }

    connectTimeout.set(Duration.ofMinutes(5))
    clientTimeout.set(Duration.ofMinutes(30))

    transitionCheckOptions {
        // We have many artifacts and Maven Central can take a long time on its compliance checks.
        // Set the timeout for waiting for the repository to close to a comfortable 50 minutes.
        maxRetries.set(300)
        delayBetween.set(Duration.ofSeconds(10))
    }
}

// Gets the git version
val gitVersion: String by lazy {
    providers
        .exec {
            isIgnoreExitValue = true
            commandLine("git", "describe", "--tags", "--always", "--dirty")
        }
        .standardOutput
        .asText
        .map { it.trim().removePrefix("r") }
        .getOrElse("UNKNOWN")
}

// Publish snapshots
tasks.register("publishSnapshots") {
    group = "publishing"
    description = "Publishes snapshots to Sonatype"

    if (version.toString().endsWith("-SNAPSHOT")) {
        dependsOn(tasks.named("publishAllPublicationsToLocalBuildRepository"))
        dependsOn(tasks.named("publishToSonatype"))
    }
}

// Publish the release
tasks.register("publishArchives") {
    group = "publishing"
    description = "Publishes a release and uploads to Sonatype / Maven Central"

    val currentGitVersion = gitVersion
    val gitVersionMatch = currentGitVersion == version
    doFirst {
        if (!gitVersionMatch) {
            val cause =
                """
                Version mismatch:
                =================

                 $version != $currentGitVersion

                 The project version does not match the git tag.
                """
                    .trimMargin()
            throw GradleException(cause)
        } else {
            println("Publishing: ${project.name} : $currentGitVersion")
        }
    }
    if (gitVersionMatch) {
        dependsOn(tasks.named("publishAllPublicationsToLocalBuildRepository"))
        dependsOn(tasks.named("publishToSonatype"))
    }
}
