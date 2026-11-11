plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cat.ghwidget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cat.ghwidget"
        minSdk = 29          // Android 10+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // 已生成的本地签名（密码固定，仅个人分发用）
    signingConfigs {
        create("release") {
            storeFile = file("signing/release.jks")
            storePassword = "ghwidget2026"
            keyAlias = "ghwidget"
            keyPassword = "ghwidget2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
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
    // 唯一第三方依赖，极致轻量
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
