import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

val versionPropsFile = file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { stream -> versionProps.load(stream) }
} else {
    versionProps["VERSION_CODE"] = "1"
    versionProps["VERSION_NAME"] = "1.0"
}

val appVersionCode = (versionProps["VERSION_CODE"] as String).toInt()
val appVersionName = versionProps["VERSION_NAME"] as String

android {
  namespace = "com.phdev.quantofalta"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.phdev.quantofalta"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  flavorDimensions += "distribution"
  
  productFlavors {
    create("playStore") {
      dimension = "distribution"
      buildConfigField("String", "API_BASE_URL", "\"https://api.tocontando.com.br\"")
      buildConfigField("String", "SITE_BASE_URL", "\"https://tocontando.com.br\"")
      buildConfigField("String", "SHARE_BASE_URL", "\"https://share.tocontando.com.br\"")
      buildConfigField("Boolean", "OTA_UPDATES_SUPPORTED", "false")
      buildConfigField("String", "OTA_RELEASE_CHANNEL", "\"playstore\"")
    }
    create("website") {
      dimension = "distribution"
      buildConfigField("String", "API_BASE_URL", "\"https://api.tocontando.com.br\"")
      buildConfigField("String", "SITE_BASE_URL", "\"https://tocontando.com.br\"")
      buildConfigField("String", "SHARE_BASE_URL", "\"https://share.tocontando.com.br\"")
      buildConfigField("Boolean", "OTA_UPDATES_SUPPORTED", "true")
      buildConfigField("String", "OTA_RELEASE_CHANNEL", "\"stable\"")
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      buildConfigField("String", "API_BASE_URL", "\"https://api.tocontando.com.br\"")
      buildConfigField("String", "SITE_BASE_URL", "\"https://tocontando.com.br\"")
      buildConfigField("String", "SHARE_BASE_URL", "\"https://share.tocontando.com.br\"")
    }
    create("benchmark") {
      initWith(getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      proguardFiles("benchmark-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    isCoreLibraryDesugaringEnabled = true
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.incremental", "true")
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation("com.android.billingclient:billing-ktx:7.0.0")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  implementation("androidx.glance:glance-appwidget:1.1.0")
  implementation("androidx.glance:glance-material3:1.1.0")
  implementation("androidx.biometric:biometric:1.2.0-alpha05")
  implementation(libs.coil.compose)
  implementation("com.vanniktech:android-image-cropper:4.6.0")
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  implementation(libs.metricsPerformance)
  // "ksp"(libs.moshi.kotlin.codegen)
  implementation(libs.kotlinx.collections.immutable)
  coreLibraryDesugaring(libs.desugar.jdk.libs)
}

tasks.register("bumpVersionCode") {
  group = "versioning"
  description = "Incrementa VERSION_CODE em app/version.properties antes de publicar um APK OTA."

  doLast {
    val props = Properties()
    if (versionPropsFile.exists()) {
      versionPropsFile.inputStream().use { props.load(it) }
    }

    val currentCode = props.getProperty("VERSION_CODE", "1").toInt()
    props.setProperty("VERSION_CODE", (currentCode + 1).toString())
    props.setProperty("VERSION_NAME", props.getProperty("VERSION_NAME", "1.0"))

    versionPropsFile.outputStream().use { output ->
      props.store(output, "Auto-incremented by Gradle")
    }

    println("VERSION_CODE ${currentCode} -> ${currentCode + 1}")
  }
}
