plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.smartride_dbd"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartride_dbd"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            assets {
                srcDirs("src\\main\\assets", "src\\main\\assets\\2",
                    "src\\main\\assets",
                    "src\\main\\assets"
                )
            }
        }
    }
}

dependencies {
    implementation ("androidx.core:core-ktx:1.10.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0") // or the latest version
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.x.x")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("org.tensorflow:tensorflow-lite:2.12.0") // Or latest version
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") // For pre/post-processing
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.airbnb.android:lottie:6.0.0") // Lottie animation library
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation ("androidx.core:core-ktx:1.10.1")
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
