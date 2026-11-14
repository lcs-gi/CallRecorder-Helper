import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Helper : enregistreur d'appels, installé à part (APK). Il détecte les appels, enregistre,
// identifie le correspondant puis remet chaque enregistrement à l'application CallRecorder.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.callrecorder.helper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.callrecorder.helper"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        // Empreintes SHA-256 acceptées pour l'application CallRecorder, en plus de la clé du Helper lui-même.
        // À remplir avec le certificat « App signing » de la Play Console (voir README). Séparées par des virgules.
        buildConfigField("String", "TRUSTED_APP_CERTS", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signé avec la clé debug pour pouvoir l'installer directement.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(project(":core"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
