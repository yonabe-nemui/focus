import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
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
        getByName("debug") {
            buildConfigField(
                "String",
                "SERVER_BASE_URL",
                "\"${providers.gradleProperty("focus.android.serverUrl").getOrElse("http://10.0.2.2:8080")}\"",
            )
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField(
                "String",
                "SERVER_BASE_URL",
                "\"${providers.gradleProperty("focus.android.serverUrl.release").getOrElse("")}\"",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf("src/main/kotlin"))
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.napier)
    debugImplementation(libs.compose.uiTooling)

}
