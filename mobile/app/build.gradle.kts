plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.cameramonitoring"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cameramonitoring"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val configuredApiUrl = providers.gradleProperty("cameraApiUrl").orElse("http://10.0.2.2:8081/api").get()
        val configuredApiKey = providers.gradleProperty("cameraApiKey").orElse("").get()
        buildConfigField("String", "API_BASE_URL", "\"$configuredApiUrl\"")
        buildConfigField("String", "API_KEY", "\"$configuredApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
