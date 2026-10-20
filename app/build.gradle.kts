plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Secret values repo me nahi rakhte: gradle property ya env (GitHub Actions secret) se aate hain.
fun prop(name: String, default: String = ""): String =
    (project.findProperty(name) as String?) ?: System.getenv(name) ?: default

android {
    namespace = "com.myfuturecode.proxygo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.myfuturecode.proxygo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // Remote config: <CONFIG_URL>?file=<package> (header X-App-Key)
        buildConfigField("String", "CONFIG_URL", "\"${prop("CONFIG_URL", "https://tirangae-online.live/sagar_apps/get_json.php")}\"")
        buildConfigField("String", "CONFIG_KEY", "\"${prop("CONFIG_APP_KEY")}\"")
        buildConfigField("String", "APPLOVIN_SDK_KEY", "\"${prop("APPLOVIN_SDK_KEY")}\"")
        // Default = Google ka test AdMob app id; Play pe daalne se pehle apna id do.
        manifestPlaceholders["admobAppId"] = prop("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.android.gms:play-services-ads:23.5.0")
    implementation("com.applovin:applovin-sdk:13.0.1")
}
