plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.liber"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.liber"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        compileSdkExtension = 19

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.viewbinding)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.material)
    implementation(libs.phosphor.icon)
    implementation(libs.coil.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.pdf.viewer)
    implementation(libs.androidx.ink.authoring)
    implementation(libs.androidx.ink.brush)
    implementation(libs.androidx.ink.rendering)
    implementation(libs.androidx.ink.strokes)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.readium.navigator)
    implementation(libs.readium.lcp)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}