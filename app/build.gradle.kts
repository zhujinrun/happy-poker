plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

import java.util.Properties

// CI 环境：从环境变量读取签名配置
val ciStoreFile = System.getenv("KEYSTORE_FILE") ?: ""
val ciStorePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
val ciKeyAlias = System.getenv("KEYSTORE_KEY_ALIAS") ?: ""
val ciKeyPassword = System.getenv("KEYSTORE_KEY_PASSWORD") ?: ""

val hasCiSigning = ciStoreFile.isNotEmpty() && ciStorePassword.isNotEmpty() && ciKeyAlias.isNotEmpty() && ciKeyPassword.isNotEmpty()

// 本地环境：从 keystore.properties 读取签名配置
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.happy.poker.app"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.happy.poker.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        getByName("debug")
        create("release") {
            if (hasCiSigning) {
                // CI 环境：使用解码后的 keystore 文件
                storeFile = file(ciStoreFile)
                storePassword = ciStorePassword
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPassword
            } else if (keystorePropertiesFile.exists()) {
                // 本地环境：使用 keystore.properties 文件
                storeFile = file(keystoreProperties.getProperty("storeFile", "release.keystore"))
                storePassword = keystoreProperties.getProperty("storePassword", "")
                keyAlias = keystoreProperties.getProperty("keyAlias", "")
                keyPassword = keystoreProperties.getProperty("keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    
    // MQTT
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    
    // 依赖注入
    implementation("io.insert-koin:koin-android:3.4.3")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
