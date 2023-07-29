import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = property("group")!!
version = property("version")!!

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.glaremasters.me/repository/bloodshot/")
    maven("https://maven.playpro.com")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("io.github.monun:kommand-core:3.1.7")

    compileOnly("com.griefdefender:api:2.1.0-SNAPSHOT")
    compileOnly("net.coreprotect:coreprotect:21.3")
    compileOnly("io.papermc.paper:paper-api:1.20.0-R0.1-SNAPSHOT")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
        kotlinOptions.javaParameters = true
    }

    withType<JavaCompile>() {
        options.encoding = "UTF-8"
    }

    withType<Javadoc>() {
        options.encoding = "UTF-8"
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        archiveVersion.set("")

        dependencies {
            include(dependency("dev.jorel:commandapi-bukkit-shade:9.0.3"))
        }

        relocate("dev.jorel.commandapi", "dev.groovin.canibuildhere.commandapi")

        from(sourceSets["main"].output)
    }
}