plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "app.focus.personal"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.focus.personal"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        // サーバー URL は gradle.properties の focus.android.serverUrl で上書き可能
        // (実機からは PC の LAN IP を指定する)。空文字列ならサーバーを介さず各ソースへ直接アクセスする。
        // focus.android.serverApiKey はサーバー側 FOCUS_API_KEY と同じ値を設定する(空なら未使用)
        val serverApiKey = providers.gradleProperty("focus.android.serverApiKey").getOrElse("")
        getByName("debug") {
            buildConfigField(
                "String",
                "SERVER_BASE_URL",
                "\"${providers.gradleProperty("focus.android.serverUrl").getOrElse("http://10.0.2.2:8080")}\"",
            )
            // デバッグメニュー(接続先切り替え)から Cloud Run を選んだときに使う URL
            buildConfigField(
                "String",
                "SERVER_BASE_URL_CLOUD",
                "\"${providers.gradleProperty("focus.android.serverUrl.release").getOrElse("")}\"",
            )
            buildConfigField("String", "SERVER_API_KEY", "\"$serverApiKey\"")
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField(
                "String",
                "SERVER_BASE_URL",
                "\"${providers.gradleProperty("focus.android.serverUrl.release").getOrElse("")}\"",
            )
            buildConfigField("String", "SERVER_API_KEY", "\"$serverApiKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // デバッグメニュー(接続先切り替え)はビルドタイプ別ソースセット(src/debug/kotlin・src/release/kotlin)で出し分ける。
    // built-in Kotlin は src/<sourceSet>/kotlin を既定で参照するため sourceSets の指定は不要
    // Kotlin の jvmTarget は built-in Kotlin では targetCompatibility に揃う

    // Android Lint: .\gradlew.bat :androidApp:lintDebug (composeApp・shared の Android 向けコードも対象)
    lint {
        checkDependencies = true
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.napier)
    debugImplementation(libs.compose.uiTooling)
    // デバッグメニュー UI 用(release には含めない)
    debugImplementation(libs.compose.foundation)
    debugImplementation(libs.compose.material3)
    debugImplementation(libs.compose.ui)
}
