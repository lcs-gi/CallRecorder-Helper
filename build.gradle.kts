// AGP 9 compile le Kotlin lui-même (plus de plugin kotlin-android). Version de Kotlin imposée ici,
// identique à celle du plugin Compose.
buildscript {
    dependencies { classpath(libs.kotlin.gradle.plugin) }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
