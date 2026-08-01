plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
    id("com.google.gms.google-services")
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (
            requested.group == "androidx.test.espresso" &&
                requested.name in setOf("espresso-core", "espresso-idling-resource") &&
                requested.version?.startsWith("3.2") == true
        ) {
            // Flutter 3.38's integration_test requests Espresso 3.2.x, whose
            // artifacts use the same namespace and are rejected by AGP 9.
            useVersion("3.7.0")
            because("Espresso 3.7.0 provides AGP 9-compatible Android namespaces")
        }
    }
}

android {
    namespace = "com.carebridge.app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.carebridge.app"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = 26
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
