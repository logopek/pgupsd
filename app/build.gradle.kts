import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val releaseStoreFile = rootProject.file("sign.jks")
val releaseStorePassword = signingValue("pgupsd.storePassword", "PGUPSD_STORE_PASSWORD")
val releaseKeyPassword = signingValue("pgupsd.keyPassword", "PGUPSD_KEY_PASSWORD")
    ?: releaseStorePassword
val releaseKeyAlias = signingValue("pgupsd.keyAlias", "PGUPSD_KEY_ALIAS") ?: "key0"
val canSignRelease = releaseStoreFile.isFile &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "ni.shikatu.pgupsd"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "ni.shikatu.pgupsd"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register<InstallPhoneTask>("installPhone") {
    group = "install"
    description = "Собрать release и поставить на подключённый телефон"
    signed.set(canSignRelease)
    adb.set(adbPath())
    applicationId.set("ni.shikatu.pgupsd")
    activity.set("ni.shikatu.pgupsd.MainActivity")
    if (canSignRelease) {
        dependsOn("assembleRelease")
        apk.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    }
}

fun signingValue(property: String, env: String): String? {
    providers.gradleProperty(property).orNull?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    providers.environmentVariable(env).orNull?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val files = listOf(
        rootProject.file("keystore.properties"),
        rootProject.file("local.properties"),
    )
    for (file in files) {
        if (!file.isFile) continue
        val props = Properties()
        file.inputStream().use { props.load(it) }
        props.getProperty(property)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return null
}

fun adbPath(): String {
    val sdk = signingValue("sdk.dir", "ANDROID_HOME")
        ?: signingValue("sdk.dir", "ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
    if (!sdk.isNullOrBlank()) {
        val fromSdk = File(sdk, "platform-tools/adb")
        if (fromSdk.canExecute()) return fromSdk.absolutePath
    }
    sequenceOf("/usr/bin/adb", "/home/userx/Android/Sdk/platform-tools/adb")
        .map(::File)
        .firstOrNull { it.canExecute() }
        ?.let { return it.absolutePath }
    return ""
}

