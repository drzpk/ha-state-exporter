plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.allopen") version "1.8.21"
    id("com.google.cloud.tools.jib") version "3.4.1"
    id("com.palantir.git-version") version "3.0.0"
}

fun projectVersion(): String {
    var version = System.getProperty("VER")
    if (version == null) {
        val gitVersion: groovy.lang.Closure<String> by extra
        version = gitVersion()

        val prefixPattern = Regex("""^v\d+\.\d+\.\d+(?:\.dirty)?$""")
        if (prefixPattern.matches(version))
            version = version.substringAfter("v")
    }

    return version
}

group = "dev.drzepka.smarthome"
version = projectVersion()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.6.4")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    implementation("com.influxdb:influxdb-client-kotlin:6.8.0")
    implementation("com.influxdb:flux-dsl:6.8.0")
    implementation("io.ktor:ktor-client-core:2.3.1")
    implementation("io.ktor:ktor-client-apache5:2.3.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.1")
    implementation("io.ktor:ktor-serialization-jackson:2.3.1")
    implementation("io.insert-koin:koin-core:3.4.2")
    implementation("io.insert-koin:koin-core-coroutines:3.4.1")
    implementation("io.insert-koin:koin-logger-slf4j:3.4.1")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.2.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation(platform("org.junit:junit-bom:5.9.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("io.insert-koin:koin-test:3.4.1")
    testImplementation("io.insert-koin:koin-test-junit4:3.4.1")
    testImplementation("org.testcontainers:testcontainers:1.18.0")
    testImplementation("org.testcontainers:junit-jupiter:1.18.0")
    testImplementation("org.testcontainers:mariadb:1.18.0")
    testImplementation("org.testcontainers:influxdb:1.18.0")
    testImplementation("com.github.tomakehurst:wiremock:3.0.0-beta-8")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.0.2")
    testImplementation("io.mockk:mockk:1.13.5")
}

allOpen {
    annotation("dev.drzepka.smarthome.haexporter.domain.util.Component")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

jib {
    from {
        image = "eclipse-temurin:17-jre-alpine"
    }
    to {
        image = "ghcr.io/drzpk/ha-state-exporter"
        tags = setOf(
            "latest",
            version.toString()
        )
        container {
            creationTime.set("USE_CURRENT_TIMESTAMP")
        }
    }
}

tasks.register("projectVersion") {
    println(projectVersion())
}
