plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ke.payhero.autodial"
    compileSdk = 35

    defaultConfig {
        applicationId = "ke.payhero.autodial"
        minSdk = 23
        targetSdk = 35
        // Each build is a newer version, so a sideloaded APK can replace the
        // installed app instead of being rejected as "App not installed".
        versionCode = (System.currentTimeMillis() / 1000L).toInt()
        versionName = "1.1"
    }

    buildTypes {
        release {
            // Same certificate as debug, so a release APK can update an
            // install that was previously sideloaded from a debug build.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
