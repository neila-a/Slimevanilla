plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
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


    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.paper.api)
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
