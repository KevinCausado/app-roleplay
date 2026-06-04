import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.elara.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elara.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // API key from local.properties — never commit
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localProps.load(localFile.inputStream())
        val openRouterKey = localProps.getProperty("OPENROUTER_API_KEY") ?: ""
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterKey\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.objectdetection)
    implementation(libs.gson)
}
