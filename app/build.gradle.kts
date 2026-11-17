plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.yadkharj"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yadkharj"
        minSdk = 26
        targetSdk = 34
        // شمارهٔ اجرای CI هر بار بزرگ‌تر می‌شود؛ بدون این، نصب نسخهٔ جدید روی قبلی خطا می‌دهد.
        val runNo = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = runNo
        versionName = "1.0.$runNo"
    }

    // امضای ثابت: اگه CI کلید ثابت (از Secret) را بدهد، همهٔ نسخه‌ها با یک کلید امضا می‌شوند و روی هم نصب می‌شوند.
    // بدون این، هر اجرای CI یک کلید debug تازه می‌سازد و اندروید نصب روی نسخهٔ قبلی را رد می‌کند.
    val ksPath = System.getenv("KEYSTORE_PATH")
    if (ksPath != null && file(ksPath).exists()) {
        signingConfigs {
            create("stable") {
                storeFile = file(ksPath)
                storeType = "pkcs12"
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
        buildTypes { getByName("debug") { signingConfig = signingConfigs.getByName("stable") } }
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
}
