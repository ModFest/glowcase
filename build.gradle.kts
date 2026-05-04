plugins {
	id("maven-publish")
	alias(libs.plugins.loom)
	alias(libs.plugins.minotaur)
}

val slug = property("slug") as String
val compatibleVersions = property("compatibleVersions") as String

version = "${property("baseVersion")}+${property("branch")}"
base {
	archivesName = slug
}

repositories {
	maven("https://maven.nucleoid.xyz/")
	maven("https://repo.sleeping.town/")
	maven("https://maven.terraformersmc.com/")
	maven("https://jitpack.io")

	maven {
		name = "BlameJared Maven (CrT / Bookshelf)"
		url = uri("https://maven.blamejared.com")
	}

	maven {
		name = "CaffeineMC Maven (Releases)"
		url = uri("https://maven.caffeinemc.net/releases")
	}

	exclusiveContent {
		forRepository {
			maven {
				name = "Cassian's Maven"
				url = uri("https://maven.cassian.cc")
			}
		}
		filter {
			includeGroupAndSubgroups("cc.cassian")
		}
	}

	mavenLocal()
}

dependencies {
	minecraft(libs.mc)

	implementation(libs.fl)
	implementation(libs.fapi)

	implementation(libs.placeholder)
	include(libs.placeholder)

//	compileOnly(libs.polydex)
//	localRuntime(libs.polydex)

	implementation(libs.kaleidoConfig)
	include(libs.kaleidoConfig)

	implementation(libs.asm)
	include(libs.asm)

	/*compileOnly(libs.emi)
	localRuntime(libs.emi)*/

	compileOnly(libs.rrv)
	localRuntime(libs.rrv)

	compileOnly(libs.modmenu)
	localRuntime(libs.modmenu)

	compileOnly(libs.sodium)
	// localRuntime(libs.sodium)
}

loom {
	accessWidenerPath.set(file("src/main/resources/glowcase.accesswidener"))

	runs {
		val clientMixinSwap = register("clientMixinSwap") {
			client()
			ideConfigGenerated(true)
			name = "Minecraft Client - (Mixin Swap)"
			source(sourceSets.main.get())
			vmArg("-Dmixin.debug.export=true")
		}

		afterEvaluate {
			try {
				val compileClasspath = project.configurations.getByName("compileClasspath")
				val resolved = compileClasspath.resolvedConfiguration.resolvedArtifacts
				val artifact = resolved.firstOrNull { it.name == "sponge-mixin" }

				if (artifact != null) {

					clientMixinSwap.get().vmArg("-javaagent:\"${artifact.file.absolutePath}\"")

					println("[Info]: Mixin Hotswap Run should be working")
				} else {
					println("[Warning]: Unable to locate file path for Mixin Jar, HotSwap Run will not work!")
				}
			} catch (e: Exception) {
				println("[Error]: Failed to setup MixinHotswap! Enable logging to view why.")
				// e.printStackTrace()
			}
		}
	}
}

fabricApi {
	configureDataGeneration {
		client = true
	}
}

tasks.processResources {
	val user = rootProject.property("user")
	val authors = rootProject.property("authors") as String
	val contributors = rootProject.property("contributors") as String
	val meta = mapOf(
		"version"			to version,
		"modId"				to rootProject.property("modId"),
		"modName"			to rootProject.property("modName"),
		"modDescription"	to rootProject.property("modDescription"),
		"homepage"			to "https://modrinth.com/mod/${slug}",
		"issues"			to "https://github.com/${user}/${slug}/issues",
		"sources"			to "https://github.com/${user}/${slug}",
		"license"			to rootProject.property("license"),
		"authors"			to authors.split(", ").joinToString("\",\n    \""),
		"contributors"		to contributors.split(", ").joinToString("\",\n    \""),
		"members"			to "$authors. Contributions by $contributors",
		"mc"				to compatibleVersions.split(", ")[0],
		"fl"				to libs.versions.fl.get(),
		"fapi"				to libs.versions.fapi.get(),
		"placeholder"		to libs.versions.placeholder.get(),
//		"polydex"			to libs.versions.polydex.get()
	)

	inputs.properties(meta)
	filesMatching("*.mod.json") { expand(meta) }
	filesMatching("META-INF/*mods.toml") { expand(meta) }
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.release = 25
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${base.archivesName}" }
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}
}

modrinth {
	val compatibleLoaders = property("compatibleLoaders") as String

	token = "${System.getenv("MODRINTH_TOKEN")}"
	projectId = slug
	versionNumber = project.version as String
	uploadFile.set(tasks.jar)
	gameVersions = compatibleVersions.split(", ").toList()
	loaders = compatibleLoaders.split(", ").toList()
	changelog = "${System.getenv("CHANGELOG")}"
	syncBodyFrom = "<!--DO NOT EDIT MANUALLY: synced from gh readme-->\n" + rootProject.file("README.md").readText()
	dependencies {
		required.version("fabric-api", libs.versions.fapi.get())
		embedded.version("placeholder-api", libs.versions.placeholder.get())
//		optional.version "polydex", libs.versions.polydex.get()
	}
}
