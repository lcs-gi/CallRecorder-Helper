// Code commun à l'application et au Helper : modèle, formats, décisions de fin d'appel, contrat d'échange, thème.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.callrecorder.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.compose.material.icons)
    api(libs.core.ktx)
    api(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
