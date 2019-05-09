/*
 * Copyright (C) 2019 Knot.x Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.nosphere.apache.rat.RatTask

group = "io.knotx"

plugins {
    id("java-library")
    id("maven-publish")
    id("jacoco")
    id("signing")
    id("org.nosphere.apache.rat") version "0.4.0"
}

repositories {
    jcenter()
    mavenLocal()
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/staging/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    implementation(platform("io.knotx:knotx-dependencies:${project.version}"))
    implementation(group = "io.vertx", name = "vertx-core")
    implementation(group = "io.vertx", name = "vertx-rx-java2")
    implementation(group = "io.vertx", name = "vertx-service-proxy")
    implementation(group = "io.vertx", name = "vertx-rx-java2")
    implementation(group = "io.vertx", name = "vertx-config")
    implementation(group = "io.vertx", name = "vertx-config-hocon")
    implementation(group = "io.vertx", name = "vertx-junit5")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation(group = "com.google.guava", name = "guava")
    implementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
    implementation(group = "org.junit.jupiter", name = "junit-jupiter-params")
    implementation(group = "org.junit.jupiter", name = "junit-jupiter-migrationsupport")
    implementation(group = "org.mockito", name = "mockito-core")
    implementation(group = "org.mockito", name = "mockito-junit-jupiter")
    implementation(group = "com.github.tomakehurst", name = "wiremock")
    implementation(group = "me.alexpanov", name = "free-port-finder")
    implementation(group = "commons-io", name = "commons-io")
    implementation(group = "org.jsoup", name = "jsoup", version = "1.11.2")

    testImplementation(group = "io.rest-assured", name = "rest-assured", version = "3.3.0")
    testImplementation(group = "io.vertx", name = "vertx-web")

    testRuntime("io.knotx:knotx-launcher")
    testRuntime(group = "org.junit.jupiter", name = "junit-jupiter-engine")
}

tasks {
    named<RatTask>("rat") {
        excludes.addAll("**/*.md", "**/build/*", "**/out/*", "**/*.conf", "**/*.json", "gradle", "gradle.properties", ".travis.yml", ".idea")
    }
    getByName("build").dependsOn("rat")
}

tasks.register<Jar>("sourcesJar") {
    from(sourceSets.named("main").get().allJava)
    classifier = "sources"
}
tasks.register<Jar>("javadocJar") {
    from(tasks.named<Javadoc>("javadoc"))
    classifier = "javadoc"
}
tasks.named<Javadoc>("javadoc") {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "knotx-junit5"
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            pom {
                name.set("Knot.x JUnit 5")
                description.set("Testing Knot.x with JUnit 5")
                url.set("http://knotx.io")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("marcinczeczko")
                        name.set("Marcin Czeczko")
                        email.set("https://github.com/marcinczeczko")
                    }
                    developer {
                        id.set("skejven")
                        name.set("Maciej Laskowski")
                        email.set("https://github.com/Skejven")
                    }
                    developer {
                        id.set("tomaszmichalak")
                        name.set("Tomasz Michalak")
                        email.set("https://github.com/tomaszmichalak")
                    }
                    developer {
                        id.set("tMaxx")
                        name.set("Mikolaj Wawrowski")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Knotx/knotx-junit5.git")
                    developerConnection.set("scm:git:ssh://github.com:Knotx/knotx-junit5.git")
                    url.set("http://knotx.io")
                }
            }
        }
        repositories {
            maven {
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                credentials {
                    username = if (project.hasProperty("ossrhUsername")) project.property("ossrhUsername")?.toString() else "UNKNOWN"
                    password = if (project.hasProperty("ossrhPassword")) project.property("ossrhPassword")?.toString() else "UNKNOWN"
                    println("Connecting with user: ${username}")
                }
            }
        }
    }
}
signing {
    sign(publishing.publications["mavenJava"])
}

apply(from = "gradle/javaAndUnitTests.gradle.kts")
apply(from = "gradle/jacoco.gradle.kts")