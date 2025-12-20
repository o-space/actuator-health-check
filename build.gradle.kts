plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.owasp.dependencycheck") version "12.1.9"
}

group = "com.chainsea"
version = "0.0.1-SNAPSHOT"
description = "health-check"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Configure Mockito to use inline mock maker to avoid agent warnings
    systemProperty("mockito.mock-maker", "mock-maker-inline")
}

tasks.register<Test>("smokeTest") {
    description = "Runs smoke tests"
    group = "verification"
    useJUnitPlatform {
        includeTags("smoke")
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("smoke", "integration")
    }
    dependsOn("smokeTest")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
}

dependencyCheck {
    // Fail build if CVSS score is 7.0 or higher
    failBuildOnCVSS = 7.0f
    formats = listOf("HTML", "JSON", "XML")
    suppressionFile = "config/dependency-check-suppressions.xml"

    analyzers {
        assemblyEnabled = false
        nuspecEnabled = false
        nodeEnabled = false
        nodeAuditEnabled = false
    }

    // Auto-update the NVD data (can be disabled for CI)
    autoUpdate = true
}
