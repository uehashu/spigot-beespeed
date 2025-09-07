version = "1.0.0"

plugins {
    java
    id("eclipse") // Eclipse用の設定を生成
    id("idea")    // IntelliJ IDEA用の設定を生成
}

java {
    // Java 21以上で動作（Spigot 1.21.4の要件）
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot SNAPSHOT
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.withType<Jar> {
    archiveFileName.set("${project.name}-${project.version}.jar")
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}
