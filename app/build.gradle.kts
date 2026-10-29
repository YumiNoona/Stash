import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingFile = providers.environmentVariable("STASH_SIGNING_PROPERTIES").orNull?.let(::file)
    ?: file("${System.getProperty("user.home")}/.android/stash-signing/signing.properties")
val releaseSigning = Properties().apply { if (signingFile.isFile) signingFile.inputStream().use(::load) }

android {
    namespace = "com.lockr.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lockr.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.0.0"
    }

    signingConfigs {
        create("production") {
            if (signingFile.isFile) {
                storeFile = file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("production")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

val validateProductionSigning by tasks.registering {
    doLast {
        require(signingFile.isFile && listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all {
            !releaseSigning.getProperty(it).isNullOrBlank()
        }) { "Configure production signing outside Git. See docs/building.md." }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateProductionSigning) }
