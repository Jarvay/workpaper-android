import java.io.File

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val androidNdkHome: String by lazy {
    val envNdk = System.getenv("ANDROID_NDK_HOME")
    if (envNdk != null && File(envNdk).exists()) {
        envNdk
    } else {
        val sdkRoot = System.getenv("ANDROID_SDK_ROOT") ?: System.getenv("ANDROID_HOME")
        if (sdkRoot != null) {
            val ndkDir = File("$sdkRoot/ndk")
            if (ndkDir.exists()) {
                ndkDir.listFiles()
                    ?.filter { it.isDirectory && it.name.matches(Regex("\\d+\\.\\d+\\.\\d+.*")) }
                    ?.maxByOrNull { it.name }
                    ?.absolutePath
                    ?: "$sdkRoot/ndk"
            } else {
                ""
            }
        } else {
            val commonPaths = listOf(
                "C:\\Android\\Sdk\\ndk",
                "D:\\Android\\Sdk\\ndk",
                "${System.getenv("LOCALAPPDATA")}\\Android\\Sdk\\ndk"
            )
            commonPaths.firstOrNull { File(it).exists() }
                ?.let { dir ->
                    File(dir).listFiles()
                        ?.filter { it.isDirectory && it.name.matches(Regex("\\d+\\.\\d+\\.\\d+.*")) }
                        ?.maxByOrNull { it.name }
                        ?.absolutePath
                }
                ?: ""
        }
    }
}

val cargoNdkBuild = tasks.register<Exec>("cargoNdkBuild") {
    group = "rust"
    description = "Build Rust library for Android using cargo-ndk"

    workingDir = file("$projectDir")
    environment["ANDROID_NDK_HOME"] = androidNdkHome

    commandLine(
        "cargo", "ndk",
        "-o", "src/main/jniLibs",
        "-t", "arm64-v8a",
        "-t", "armeabi-v7a",
        "-t", "x86_64",
        "build", "--release"
    )

    outputs.dir(file("src/main/jniLibs"))
}

tasks.matching { it.name.startsWith("assemble") }.configureEach {
    dependsOn(cargoNdkBuild)
}

android {
    namespace = "jarvay.workpaper.rust"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}