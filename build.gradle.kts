plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "eu.greev"
version = "1.1.2"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation("net.dv8tion", "JDA", "5.0.0-beta.13") {
        exclude("club.minnced", "opus-java")
    }
    implementation("org.slf4j", "slf4j-log4j12", "2.0.1")
    implementation("org.apache.logging.log4j", "log4j-api", "2.19.0")
    implementation("org.apache.logging.log4j", "log4j-core", "2.19.0")
    implementation("me.carleslc.Simple-YAML", "Simple-Yaml", "1.8.3")
    implementation("com.deepl.api", "deepl-java", "1.3.0")

    compileOnly("org.projectlombok", "lombok", "1.18.24")
    annotationProcessor("org.projectlombok", "lombok", "1.18.24")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    shadowJar {
        manifest {
            attributes["Main-Class"] = "eu.greev.translator.Main"
        }
        archiveFileName.set("discord-translator-bot.jar")
    }
}