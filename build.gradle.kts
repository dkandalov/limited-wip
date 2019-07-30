import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.internal.HasConvention
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    idea
    java
    kotlin("jvm").version("1.1.1")
    id("org.jetbrains.intellij").version("0.4.9")
}
java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}
repositories {
    mavenCentral()
}

fun sourceRoots(block: SourceSetContainer.() -> Unit) = sourceSets.apply(block)
val SourceSet.kotlin: SourceDirectorySet
    get() = (this as HasConvention).convention.getPlugin<KotlinSourceSet>().kotlin
var SourceDirectorySet.sourceDirs: Iterable<File>
    get() = srcDirs
    set(value) { setSrcDirs(value) }

sourceRoots {
    getByName("main") {
        java.srcDirs("./src")
        kotlin.srcDirs("./src")
        resources.srcDirs("./resources")
    }
    getByName("test") {
        java.srcDirs("./test")
        kotlin.srcDirs("./test")
    }
}

dependencies {
    testCompile("org.mockito:mockito-inline:2.25.1")
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.1"
        languageVersion = "1.1"
        // Compiler flag to allow building against pre-released versions of Kotlin
        // because IJ EAP can be built using pre-released Kotlin but it's still worth doing to check API compatibility
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
}

configure<IntelliJPluginExtension> {
    // See https://www.jetbrains.com/intellij-repository/releases for a list of available IDEA builds
    val ideVersion = System.getenv().getOrDefault("LIMITED_WIP_PLUGIN_IDEA_VERSION",
        "IC-181.3870.7"
//        "LATEST-EAP-SNAPSHOT"
    )
    println("Using ide version: $ideVersion")
    version = ideVersion
    pluginName = "LimitedWIP"
    downloadSources = true
    sameSinceUntilBuild = false
    updateSinceUntilBuild = false
}
