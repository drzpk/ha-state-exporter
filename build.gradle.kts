plugins {
    kotlin("jvm") version "1.8.21"
}

group = "dev.drzepka.smarthome"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.3"))
	testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}


tasks.test {
    useJUnitPlatform()
    testLogging {
		events("passed", "skipped", "failed")
	}
}
