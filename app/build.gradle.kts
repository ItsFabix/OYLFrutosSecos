plugins {
    jacoco
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.oylfrutossecos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.oylfrutossecos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    defaultConfig {
        val apiUrl = providers.gradleProperty("oylApiBaseUrl").getOrElse("http://10.0.2.2:8080/")
        require(apiUrl.matches(Regex("https?://[a-zA-Z0-9.:-]+/"))) { "oylApiBaseUrl debe ser una URL base http(s) terminada en /" }
        buildConfigField("String", "OYL_API_BASE_URL", "\"$apiUrl\"")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:3.14.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Escaneo QR mediante Google Code Scanner.
dependencies {
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
}

// Mapa nativo OpenStreetMap.
dependencies {
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}

 
// Cobertura de las clases de lógica (no de UI Compose generada).
// Ejecutar :app:jacocoDebugLogicReport después de testDebugUnitTest.
jacoco {
    toolVersion = "0.8.13"
}
// Robolectric loads Android-backed logic through a sandbox classloader without a source location.
// Instrument it as well; the report's evaluated packages remain unchanged.
tasks.withType<Test>().configureEach {
    extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
tasks.register<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoDebugLogicReport") {
    dependsOn("testDebugUnitTest")
    executionData.setFrom(
        fileTree(layout.buildDirectory.dir("outputs/unit_test_code_coverage/debugUnitTest")) {
            include("**/*.exec", "**/*.ec")
        }
    )
    classDirectories.setFrom(files(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            include("**/domain/**", "**/data/repository/**", "**/ui/viewmodel/**")
        },
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
            include("**/domain/**", "**/data/repository/**", "**/ui/viewmodel/**")
        }
    ))
    sourceDirectories.setFrom(files("src/main/java"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
