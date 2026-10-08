import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val localConfiguration = Properties().apply {
    val configurationFile = rootProject.file("local.properties")
    if (configurationFile.exists()) configurationFile.inputStream().use(::load)
}
val mapsApiKey = providers.environmentVariable("MAPS_API_KEY")
    .orElse(providers.gradleProperty("MAPS_API_KEY"))
    .orElse(localConfiguration.getProperty("MAPS_API_KEY", ""))
val aiFirebaseProjectId = providers.environmentVariable("AI_FIREBASE_PROJECT_ID")
    .orElse(providers.gradleProperty("AI_FIREBASE_PROJECT_ID"))
    .orElse(localConfiguration.getProperty("AI_FIREBASE_PROJECT_ID", ""))
val aiFirebaseAppId = providers.environmentVariable("AI_FIREBASE_APP_ID")
    .orElse(providers.gradleProperty("AI_FIREBASE_APP_ID"))
    .orElse(localConfiguration.getProperty("AI_FIREBASE_APP_ID", ""))
val aiFirebaseApiKey = providers.environmentVariable("AI_FIREBASE_API_KEY")
    .orElse(providers.gradleProperty("AI_FIREBASE_API_KEY"))
    .orElse(localConfiguration.getProperty("AI_FIREBASE_API_KEY", ""))

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val requiredAiFirebaseProjectId = aiFirebaseProjectId.get().trim().also {
    require(it.isNotBlank()) {
        "Set AI_FIREBASE_PROJECT_ID in local.properties, a Gradle property or the CI environment."
    }
}
val requiredAiFirebaseAppId = aiFirebaseAppId.get().trim().also {
    require(it.isNotBlank()) {
        "Set AI_FIREBASE_APP_ID in local.properties, a Gradle property or the CI environment."
    }
}
val requiredAiFirebaseApiKey = aiFirebaseApiKey.get().trim().also {
    require(it.isNotBlank()) {
        "Set AI_FIREBASE_API_KEY in local.properties, a Gradle property or the CI environment."
    }
}

android {
    namespace = "com.ahmetkaragunlu.financeai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ahmetkaragunlu.financeai"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.ahmetkaragunlu.financeai.FinanceTestRunner"
        buildConfigField("String", "AI_FIREBASE_PROJECT_ID", buildConfigString(requiredAiFirebaseProjectId))
        buildConfigField("String", "AI_FIREBASE_APP_ID", buildConfigString(requiredAiFirebaseAppId))
        buildConfigField("String", "AI_FIREBASE_API_KEY", buildConfigString(requiredAiFirebaseApiKey))
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey.get().also {
            require(it.isNotBlank()) { "Set MAPS_API_KEY in local.properties, a Gradle property or the CI environment." }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.gson)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.ai)
    debugImplementation(libs.firebase.appcheck.debug)
    releaseImplementation(libs.firebase.appcheck.playintegrity)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    implementation(libs.play.location)
    implementation(libs.play.maps)
    implementation(libs.maps.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
