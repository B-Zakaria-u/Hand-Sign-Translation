plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // alias(libs.plugins.google.services)  // Firebase: uncomment
}

android {
    namespace   = "com.handsign.poc"
    compileSdk  = 36

    defaultConfig {
        applicationId   = "com.handsign.poc"
        minSdk          = 26
        targetSdk       = 36
        versionCode     = 1
        versionName     = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Force ARM64 so that x86_64 emulators (API 30+) use their built-in ARM translation (Houdini).
        // This is required because MediaPipe Tasks Vision does not ship with x86_64 native libraries.
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose     = false
        aidl        = false
        shaders     = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources.excludes += listOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "/META-INF/{AL2.0,LGPL2.1}"
        )
        jniLibs.pickFirsts += listOf(
            "lib/**/libc++_shared.so",
            "lib/**/libfbjni.so"
        )
    }

    // Prevent AAPT from compressing native model files
    androidResources {
        noCompress += listOf("tflite", "task", "onnx", "ptl", "lite")
    }

    // Room schema export directory
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental",    "true")
    }
}

dependencies {
    // ── Core ──────────────────────────────────────────────────────────
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.coroutines.android)

    // ── Lifecycle ──────────────────────────────────────────────────────
    implementation(libs.lifecycle.vm.ktx)
    implementation(libs.lifecycle.rt.ktx)
    implementation(libs.lifecycle.livedata)

    // ── Navigation ─────────────────────────────────────────────────────
    implementation(libs.nav.fragment)
    implementation(libs.nav.ui)

    // ── CameraX ────────────────────────────────────────────────────────
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // ── MediaPipe (Hand Landmark + GestureRecognizer task) ────────────
    implementation(libs.mediapipe.tasks)

    // ── TFLite ─────────────────────────────────────────────────────────
    implementation(libs.tflite.core)
    implementation(libs.tflite.gpu)
    implementation(libs.tflite.support)

    // ── ONNX Runtime ───────────────────────────────────────────────────
    implementation(libs.onnx.runtime)

    // ── PyTorch Mobile ─────────────────────────────────────────────────
    implementation(libs.torch.mobile)

    // ── Room (SQLite) ──────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Hilt DI ────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // ── WorkManager ────────────────────────────────────────────────────
    implementation(libs.work.runtime.ktx)
    implementation(libs.startup)

    // ── Network ────────────────────────────────────────────────────────
    implementation(libs.okhttp)
    implementation(libs.gson)

    // ── DataStore ──────────────────────────────────────────────────────
    implementation(libs.datastore)
    implementation(libs.datastore.core)

    // ── Firebase (STUB) ────────────────────────────────────────────────
    // implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    // implementation("com.google.firebase:firebase-firestore-ktx")
    // implementation("com.google.firebase:firebase-auth-ktx")

    // ── Tests ──────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
}
