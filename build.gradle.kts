import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
	id("org.springframework.boot") version "2.3.5.RELEASE"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	id("org.liquibase.gradle") version "2.0.4"
	kotlin("jvm") version "1.4.10"
	kotlin("plugin.spring") version "1.4.10"
	id("org.jetbrains.kotlin.plugin.jpa") version "1.4.10"
	id("org.sonarqube") version "3.0"
	jacoco
}


group = "de.inw.serpent"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

var profile = "dev,default"
if (project.hasProperty("prod")) {
	apply("gradle/profile_prod.gradle")
} else {
	apply("gradle/profile_dev.gradle")
}

extra["testcontainersVersion"] = "1.15.0"

val liquibaseRuntime by configurations

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("javax.cache:cache-api")
	implementation("org.ehcache:ehcache")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.liquibase:liquibase-core:4.1.1")
	implementation("org.springframework.session:spring-session-jdbc")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.jetbrains.kotlin:kotlin-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
	liquibaseRuntime("org.jetbrains.kotlin:kotlin-reflect")
	liquibaseRuntime("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	liquibaseRuntime("org.springframework.boot:spring-boot-starter-data-jpa")
	liquibaseRuntime("org.liquibase:liquibase-core:4.1.1")
	liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:4.1.1")
	liquibaseRuntime("org.postgresql:postgresql")
	liquibaseRuntime("ch.qos.logback:logback-core:1.2.3")
	liquibaseRuntime("ch.qos.logback:logback-classic:1.2.3")
	liquibaseRuntime("org.yaml:snakeyaml:1.24")
	liquibaseRuntime(sourceSets["main"].output)
}

dependencyManagement {
	imports {
		mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
	dependsOn(tasks.test) // tests are required to run before generating the report
	reports {
		xml.isEnabled = true
	}
}

tasks.diffChangeLog {
	dependsOn(tasks.compileKotlin)
	dependsOn(tasks.compileJava)
}

tasks.diff {
	dependsOn(tasks.compileKotlin)
	dependsOn(tasks.compileJava)
}

if (!project.hasProperty("runList")) {
	project.extra["runList"] = "main"
}

val changelogBasePath = "src/main/resources/db/changelog/"
project.extra["diffChangelogFile"] = changelogBasePath + "changelogs/${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}_changelog.yaml"
project.extra["mainChangeLogFile"] = "db/changelog/master.yaml"

liquibase {
	activities.register("diff") {
		this.arguments = mapOf(
				"driver" to "org.postgresql.Driver",
				"url" to "jdbc:postgresql://localhost:6432/serp",
				"referenceUrl" to "hibernate:spring:de.inw.serpent.serpback?dialect=org.hibernate.dialect.PostgreSQLDialect&hibernate.physical_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy&hibernate.implicit_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy",
				"username" to "serpant",
				"password" to "serpent_dev",
				"changeLogFile" to project.extra["diffChangelogFile"],
				"defaultSchemaName" to "public",
				"logLevel" to "debug",
				"classpath" to "$buildDir/classes/kotlin/main"
		)
	}
	activities.register("main") {
		this.arguments = mapOf(
				"driver" to "org.postgresql.Driver",
				"url" to "jdbc:postgresql://localhost:6432/serp",
				"username" to "serpant",
				"password" to "serpent_dev",
				"changeLogFile" to project.extra["mainChangeLogFile"],
				"logLevel" to "debug"
		)
	}
	runList = project.ext["runList"]
}
