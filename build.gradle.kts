plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    id("maven-publish")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.22"
}

repositories {
	System.getenv("IS_GITHUB_ACTION") ?: repositories {
        maven {
			url = uri("https://maven.aliyun.com/repository/public/")
		}
        maven {
			url = uri("https://maven.aliyun.com/repository/spring/")
		}
        maven {
			url = uri("https://maven.aliyun.com/repository/google/")
		}
        maven {
			url = uri("https://maven.aliyun.com/repository/gradle-plugin/")
		}
        maven {
			url = uri("https://maven.aliyun.com/repository/spring-plugin/")
		}
        maven {
			url = uri("https://maven.aliyun.com/repository/grails-core/")
		}
        maven {
			url = uri("https://maven.aliyun.com/repository/apache-snapshots/")
		}
	}

    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle("26.2.build.+")
    compileOnly(libs.slimefun)

    implementation(libs.kotlin.stdlib)
}

kotlin {
    jvmToolchain(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/neila-a/Slimevanilla")
            credentials {
                username = "neila-a"
                password = project.findProperty("gpr.key") as String?
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            groupId = project.group.toString()
            artifactId = "slimevanilla"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
