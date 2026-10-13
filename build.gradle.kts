import java.net.URI

plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
}

repositories {
	exclusiveContent { // Sable
        forRepository {
            maven {
                url = URI("https://maven.ryanhcode.dev/releases")
                name = "RyanHCode Maven"
            }
        }
        filter {
            includeGroup("dev.ryanhcode.sable")
            includeGroup("dev.ryanhcode.sable-companion")
        }
    }
	maven {
		name = "Fuzs Mod Resources"
		url = URI("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
	}
    maven {
		name = "Modrinth"
		url = URI("https://api.modrinth.com/maven")
	}
	maven {
        name = "WispForest"
        url = URI("https://maven.wispforest.io/releases")
    }
	maven {
		name = "BlameJared"
		url = URI("https://maven.blamejared.com")
	}
    mavenCentral()
}

base {
	archivesName = providers.gradleProperty("archives_base_name")
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	modApi("dev.ryanhcode.sable:sable-fabric-${providers.gradleProperty("minecraft_version").get()}:${providers.gradleProperty("sable_version").get()}") {
		exclude(group = "fuzs.forgeconfigapiport:forgeconfigapiport")
	}
	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modCompileOnly("io.wispforest:accessories-fabric:${providers.gradleProperty("accessories_version").get()}")
    modCompileOnly("maven.modrinth:jade:${providers.gradleProperty("jade_version").get()}")
	modImplementation("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:21.1.6")
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
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
