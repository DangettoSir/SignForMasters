plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.cdt"
version = "0.0.1-SNAPSHOT"
description = "Education task for Dmitry"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.telegram:telegrambots-spring-boot-starter:6.8.0")
	implementation("org.telegram:telegrambots-meta:6.8.0")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // Exposed + PostgreSQL
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.55.0")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
    // Flyway
    implementation("org.flywaydb:flyway-core:10.17.2")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
