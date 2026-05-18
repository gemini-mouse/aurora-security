import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun quoted(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}

android {
    namespace = "app.aurorasecurity.security"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.aurorasecurity.security"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.2.1"

        buildConfigField(
            "String",
            "TELEGRAM_ALERT_API_URL",
            quoted(localProperties.getProperty("telegram.alert.apiUrl", "")),
        )
        buildConfigField(
            "String",
            "TELEGRAM_ALERT_API_TOKEN",
            quoted(localProperties.getProperty("telegram.alert.apiToken", "")),
        )
        buildConfigField(
            "String",
            "TELEGRAM_BOT_NAME",
            quoted(localProperties.getProperty("telegram.bot.name", "")),
        )
        buildConfigField(
            "String",
            "TELEGRAM_DEVICE_NAME",
            quoted(localProperties.getProperty("telegram.deviceName", "Phone")),
        )
        buildConfigField(
            "String",
            "FCM_PROJECT_ID",
            quoted(localProperties.getProperty("fcm.projectId", "")),
        )
        buildConfigField(
            "String",
            "FCM_APPLICATION_ID",
            quoted(localProperties.getProperty("fcm.applicationId", "")),
        )
        buildConfigField(
            "String",
            "FCM_API_KEY",
            quoted(localProperties.getProperty("fcm.apiKey", "")),
        )
        buildConfigField(
            "String",
            "FCM_GCM_SENDER_ID",
            quoted(localProperties.getProperty("fcm.gcmSenderId", "")),
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    val firebaseBom = platform("com.google.firebase:firebase-bom:33.1.2")

    implementation(composeBom)
    implementation(firebaseBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.11.0")
    implementation("com.google.android.gms:play-services-tflite-java:16.4.0")
    implementation("com.google.android.gms:play-services-tflite-gpu:16.4.0")
    implementation("com.google.android.gms:play-services-tflite-support:16.4.0")
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
