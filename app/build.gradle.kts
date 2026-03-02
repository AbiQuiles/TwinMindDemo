
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) //Kotlin Serialization
    alias(libs.plugins.devtool.ksp) //Kotlin Annotation Precessing Tool
    alias(libs.plugins.dagger.hilt) //Hilt
    alias(libs.plugins.android.room) //Room
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.aquiles.twinminddemo"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.aquiles.twinminddemo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties["GEMINI_API_KEY"] ?: ""}\""
        )

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    //Material 3 Icons
    implementation(libs.androidx.material.icons.core)

    //Android Compose ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    //Jetpack Compose Navigation
    implementation(libs.navigation.compose)

    //Dagger - Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)

    //Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation (libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.viewmodel)// Coroutines Lifecycle Scope

    //Kotlin Serialization Json
    implementation(libs.kotlinx.serialization.json)

    //Room
    implementation(libs.room.runtime.android)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.hilt.common)
    implementation(libs.generativeai)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.text)
    ksp(libs.room.android.compiler)

    //Retrofit
    implementation(libs.retrofit)
    //OkHttp
    implementation(libs.okhttp)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    //JSON Converter - Square GSON
    implementation(libs.gson)

    //Annotation Processing Tool (Use for Room, Hilt and anything that uses annotations
    ksp(libs.hilt.android.compiler)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}