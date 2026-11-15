// AGP 9 compile le Kotlin lui-même (plus de plugin kotlin-android). Version de Kotlin imposée ici,
// identique à celle du plugin Compose.
buildscript {
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21") }
}

plugins {
    id("com.android.application") version "9.5.1" apply false
    id("com.android.library") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
