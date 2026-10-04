plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.maskan.mobileapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.maskan.mobileapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 9
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        // Dev and UAT intentionally share the com.maskan.mobileapp.dev app
        // registration/applicationId (only a second Firebase Android app
        // could be registered) — they differ only in which Firestore
        // database they read/write. They cannot be installed side by side
        // on the same device; the last one installed wins.
        create("dev") {
            dimension = "environment"
            applicationId = "com.maskan.mobileapp.dev"
            buildConfigField("String", "FIRESTORE_DATABASE_ID", "\"maskan-dev\"")
            buildConfigField("String", "STORAGE_BUCKET_URL", "\"gs://project-c1aab7fe-8aca-4756-82a-dev\"")
        }
        create("uat") {
            dimension = "environment"
            applicationId = "com.maskan.mobileapp.dev"
            buildConfigField("String", "FIRESTORE_DATABASE_ID", "\"maskan-uat\"")
            buildConfigField("String", "STORAGE_BUCKET_URL", "\"gs://project-c1aab7fe-8aca-4756-82a-uat\"")
        }
        create("prod") {
            dimension = "environment"
            // Uses defaultConfig's applicationId (com.maskan.mobileapp) as-is.
            buildConfigField("String", "FIRESTORE_DATABASE_ID", "\"maskan-prod\"")
            buildConfigField("String", "STORAGE_BUCKET_URL", "\"gs://project-c1aab7fe-8aca-4756-82a.firebasestorage.app\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.config)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // billing-ktx pulls play-services-base, which transitively depends on the
    // long-outdated androidx.fragment:fragment:1.1.0. No fragment API is used
    // directly here — this constraint only raises the transitive version.
    constraints {
        implementation(libs.androidx.fragment) {
            because("play-services-base (via billing-ktx) resolves an outdated androidx.fragment:fragment:1.1.0")
        }
    }
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}