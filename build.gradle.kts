// AGP carga Compress 1.21 en el classloader padre. Boot 3.5.7 requiere 1.27.1
// para bootJar; alineamos el classpath de plugins, sin añadirlo al APK.
buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("org.apache.commons:commons-compress:1.27.1") }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
