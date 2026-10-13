import java.net.URI

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
}

repositories {
	flatDir {
        dirs("libs")
    }
	maven {
		name = "Modrinth"
		url = URI("https://api.modrinth.com/maven")
	}
    mavenCentral()
}

base {
	archivesName = providers.gradleProperty("archives_base_name")
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	// Sodium
	implementation("maven.modrinth:AANobbMI:v4PSXean")
	// Sable
	implementation(":sable-fabric-26.3:3.0.0")
	implementation("maven.modrinth:jade:${providers.gradleProperty("jade_version").get()}")
	implementation(files("libs/forgeconfigapiport-fabric-26.3.1.jar"))
	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

sourceSets {
	main {
		java {
			exclude("com/moigferdsrte/ragdoll/neoforge/**")
			exclude("com/moigferdsrte/ragdoll/mob/network/**")
			exclude("com/moigferdsrte/ragdoll/physics/RagdollCuriosEquipmentHelper.java")
		}
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
