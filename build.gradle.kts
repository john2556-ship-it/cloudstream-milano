import com.android.build.api.dsl.LibraryExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// Versions match the current CloudStream app (AGP 9.1.1, Kotlin 2.4.0, Gradle 9.4.1).
// The cloudstream.jar stubs are compiled with Kotlin 2.4, so an older Kotlin here fails with
// "metadata version 2.4.0 is incompatible".
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        // CloudStream gradle plugin, pinned to the commit that moved to AGP 9.1 / Kotlin 2.4
        classpath("com.github.recloudstream:gradle:32895aedb6")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) =
    extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: LibraryExtension.() -> Unit) =
    extensions.getByName<LibraryExtension>("android").configuration()

subprojects {
    // AGP 9 compiles Kotlin by itself, so "kotlin-android" must NOT be applied any more
    apply(plugin = "com.android.library")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        // on GitHub Actions this is "<your-github-name>/<repo>", filled in automatically
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "user/repo")
        authors = listOf("Milano")
    }

    android {
        namespace = "com.milano"
        compileSdk = 36
        defaultConfig {
            minSdk = 21
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8) // Required
            freeCompilerArgs.addAll(
                listOf(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions"
                )
            )
        }
    }

    dependencies {
        val cloudstream by configurations
        val implementation by configurations

        // Stubs for all CloudStream classes
        cloudstream("com.lagradost:cloudstream3:pre-release")

        implementation(kotlin("stdlib"))
        implementation("com.github.Blatzar:NiceHttp:0.4.18")
        implementation("org.jsoup:jsoup:1.22.2")
        // Do not bump Jackson above 2.13.1 (breaks older Android TV / Fire TV devices)
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
