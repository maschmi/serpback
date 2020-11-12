import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
	id("org.springframework.boot") version "2.3.5.RELEASE"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	id("org.liquibase.gradle") version "2.0.3"
	kotlin("jvm") version "1.3.72"
	kotlin("plugin.spring") version "1.3.72"
	kotlin("plugin.jpa") version "1.3.72"
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
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("javax.cache:cache-api")
	implementation("org.ehcache:ehcache")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.liquibase:liquibase-core")
	implementation("org.springframework.session:spring-session-core")
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
	liquibaseRuntime("org.springframework.boot:spring-boot-starter-data-jpa")
	liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:3.10.0")
	liquibaseRuntime("org.postgresql:postgresql")
	liquibaseRuntime("ch.qos.logback:logback-core:1.2.3")
	liquibaseRuntime("ch.qos.logback:logback-classic:1.2.3")
	liquibaseRuntime("org.yaml:snakeyaml:1.15")
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
				"url" to "jdbc:postgresql://localhost:6432/sep",
				"referenceUrl" to "hibernate:spring:de.immernurwollen.planets.domain?dialect=org.hibernate.dialect.PostgreSQL95Dialect&hibernate.physical_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy&hibernate.implicit_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy",
				"username" to "serpant",
				"password" to "serpent_dev",
				"changeLogFile" to project.extra["diffChangelogFile"],
				"defaultSchemaName" to "",
				"logLevel" to "debug"
		)
	}
	activities.register("main") {
		this.arguments = mapOf(
				"driver" to "org.postgresql.Driver",
				"url" to "jdbc:postgresql://localhost:6432/sep",
				"username" to "serpant",
				"password" to "serpent_dev",
				"changeLogFile" to project.extra["mainChangeLogFile"],
				"logLevel" to "debug"
		)
	}
	runList = project.ext["runList"]
}
