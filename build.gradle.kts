// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // STEP 1:
    // 🔧 KSP (Kotlin Symbol Processing) plugin for annotation processing
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false

    // 💉 Dagger Hilt Plugin for Dependency Injection
    id("com.google.dagger.hilt.android") version "2.52" apply false
}