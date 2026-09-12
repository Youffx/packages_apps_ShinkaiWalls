plugins {
    id("com.android.application") version "8.7.3" apply true
    id("org.jetbrains.kotlin.android") version "2.0.21" apply true

}

android {
    namespace = "com.shinkai.wallpapers"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shinkai.wallpapers"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
            res.srcDirs("res")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

data class NativeTarget(
    val triple: String,
    val abi: String
)

val nativeTargets = listOf(
    NativeTarget("aarch64-linux-android", "arm64-v8a"),
    NativeTarget("armv7-linux-androideabi", "armeabi-v7a"),
    NativeTarget("x86_64-linux-android", "x86_64"),
    NativeTarget("i686-linux-android", "x86")
)

tasks.register("buildRustRelease") {
    description = "Build Rust native library for all Android targets"
    group = "build"

    doLast {
        val jniLibsDir = file("src/main/jniLibs")
        jniLibsDir.mkdirs()

        for (target in nativeTargets) {
            val targetDir = jniLibsDir.resolve(target.abi)
            targetDir.mkdirs()

            println("Building for ${target.abi} (${target.triple})...")

            exec {
                commandLine(
                    "cargo", "ndk",
                    "--target", target.triple,
                    "--platform", "31",
                    "--output-dir", targetDir.absolutePath,
                    "--", "build", "--release"
                )
                workingDir = file("rust")
                environment("RUSTFLAGS", "-C link-arg=-s")
            }

            println("  -> ${targetDir.resolve("libshinkai.so").absolutePath}")
        }

        println("Rust build complete.")
    }
}

tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("JniLibFolders")) {
        dependsOn("buildRustRelease")
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
