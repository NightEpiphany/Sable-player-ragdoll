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
	// Fabric callbacks, registries and play networking are required by this port.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modCompileOnly("io.wispforest:accessories-fabric:${providers.gradleProperty("accessories_version").get()}")
    modCompileOnly("maven.modrinth:jade:${providers.gradleProperty("jade_version").get()}")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }

loom {
    mods {
        register("sable_player_ragdoll") { sourceSet(sourceSets.main.get()) }
    }
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "sable_player_ragdoll_test"
        enableClientGameTests = false
    }
}

loom {
    runs {
        create("smokeClient") {
            client()
            source(sourceSets["gametest"])
            runDir("build/run/smokeClient")
            vmArg("-Dragdoll.smoke=true")
            programArgs("--width", "960", "--height", "540")
        }
    }
}

tasks.named("runSmokeClient") {
    val marker = layout.buildDirectory.file("run/smokeClient/smoke-success.txt")
    doFirst { marker.get().asFile.delete() }
    doLast { check(marker.get().asFile.isFile) { "Client smoke test failed; inspect build/run/smokeClient/logs/latest.log" } }
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

// The NeoForge sources are kept in the repository as the reference
// implementation, but are not part of the Fabric source set.
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
