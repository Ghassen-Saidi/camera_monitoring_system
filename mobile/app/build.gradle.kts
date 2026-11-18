plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun asJavaStringLiteral(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val debugApiUrl = providers.gradleProperty("cameraApiUrl")
    .orElse("http://10.0.2.2:8081/api")
    .get()
val releaseApiUrl = providers.gradleProperty("cameraReleaseApiUrl")
    .orElse("")
    .get()
val configuredApiKey = providers.gradleProperty("cameraApiKey")
    .orElse("")
    .get()

android {
    namespace = "com.example.cameramonitoring"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cameramonitoring"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", asJavaStringLiteral(debugApiUrl))
        buildConfigField("String", "API_KEY", asJavaStringLiteral(configuredApiKey))
    }

    buildTypes {
        debug {
            // Debug manifest overlay explicitly permits local development HTTP.
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", asJavaStringLiteral(releaseApiUrl))
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

// Release variants must not be built with an empty or insecure API endpoint.
// Validate after Gradle has constructed the task graph, so debug builds remain usable.
gradle.taskGraph.whenReady { taskGraph ->
    val includesRelease = taskGraph.allTasks.any {
        it.name.contains("Release", ignoreCase = true)
    }
    if (includesRelease) {
        require(releaseApiUrl.startsWith("https://", ignoreCase = true)) {
            "Release builds require -PcameraReleaseApiUrl=https://your-host/api"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
