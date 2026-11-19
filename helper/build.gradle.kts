// Helper : enregistreur d'appels, installé à part (APK). Il détecte les appels, enregistre,
// identifie le correspondant puis remet chaque enregistrement à l'application CallRecorder.
import java.util.Properties

// Signature release : CallRecorder-Helper/keystore.properties (ignoré par git), keystore hors du dépôt.
// Sans ce fichier, ou avec -PdebugSigning, le Helper est signé avec la clé debug (développement).
val keystoreProps = Properties().apply {
    val file = project.file("../keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val useReleaseKey = keystoreProps.getProperty("storeFile") != null && !providers.gradleProperty("debugSigning").isPresent

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.callrecorder.helper"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lcsgi.callrecorder.helper"
        minSdk = 26
        targetSdk = 36
        versionCode = 10000
        versionName = "1.0.0"
        // Empreintes SHA-256 acceptées pour l'application CallRecorder, en plus de la clé du Helper lui-même.
        // Séparées par des virgules. Aujourd'hui : clé d'upload (builds release locaux de l'application).
        // À ajouter avant la première release publique : certificat « Clé de signature d'application » de la
        // Play Console (Play App Signing), voir README.
        buildConfigField(
            "String", "TRUSTED_APP_CERTS",
            "\"11:3B:71:26:AC:84:6B:20:A8:1D:49:00:1A:DB:1D:FD:81:3F:2F:6C:9F:1B:D8:40:16:E1:BA:5C:A4:99:02:F9\"",
        )
    }

    signingConfigs {
        if (useReleaseKey) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (useReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    implementation(project(":core"))
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
