plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zenn889.putar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zenn889.putar"
        minSdk = 24
        targetSdk = 36
        versionCode = 28
        versionName = "2.10.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(
                project.findProperty("putarStoreFile") as String?
                    ?: error("putarStoreFile belum di-set di ~/.gradle/gradle.properties")
            )
            storePassword = project.findProperty("putarStorePass") as String?
            keyAlias = project.findProperty("putarKeyAlias") as String? ?: "putar"
            keyPassword = project.findProperty("putarKeyPass") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    // pemutar media (ExoPlayer + kontrol notifikasi/lock screen)
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-session:1.7.1")
    implementation("androidx.media3:media3-common:1.7.1")
    implementation("androidx.media3:media3-ui:1.7.1")

    // album art dari MediaStore
    implementation("io.coil-kt:coil-compose:2.7.0")
}
