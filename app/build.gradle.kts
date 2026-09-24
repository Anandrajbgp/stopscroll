import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

fun signingProp(envName: String, propName: String): String? =
    System.getenv(envName) ?: keystoreProperties.getProperty(propName)

android {
    namespace = "com.anandrajbgp.stopscroll"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.anandrajbgp.stopscroll"
        minSdk = 24
        targetSdk = 37
        versionCode = 10100
        versionName = "1.1.0"

        buildConfigField(
            "String",
            "GITHUB_RELEASES_API_URL",
            "\"https://api.github.com/repos/Anandrajbgp/stopscroll/releases/latest\""
        )
        buildConfigField(
            "String",
            "FALLBACK_APK_URL",
            "\"https://github.com/Anandrajbgp/stopscroll/releases/latest\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingProp("KEYSTORE_PATH", "storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = signingProp("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingProp("KEY_ALIAS", "keyAlias")
                keyPassword = signingProp("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
