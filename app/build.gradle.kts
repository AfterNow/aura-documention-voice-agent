plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "com.afternow.aura.manuals"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.afternow.aura.manuals"
  minSdk = 34
  targetSdk = 34
  versionCode = 1
  versionName = "0.1.0"
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true }
}
dependencies {
 testImplementation("junit:junit:4.13.2")
 implementation("androidx.xr.compose:compose:1.0.0-alpha13")
 implementation(platform("androidx.compose:compose-bom:2025.07.00"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
