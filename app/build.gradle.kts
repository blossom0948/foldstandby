plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.blossom.foldstand"
    compileSdk = 37

    val configuredVersionName = providers.gradleProperty("foldstandVersionName").orElse("1.4.0")
    val configuredVersionCode = providers.gradleProperty("foldstandVersionCode").orElse("10400")

    defaultConfig {
        applicationId = "com.blossom.foldstand"
        minSdk = 26
        targetSdk = 37
        versionCode = configuredVersionCode.get().toInt()
        versionName = configuredVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("direct") {
            dimension = "distribution"
            buildConfigField("Boolean", "NOTIFICATION_ACCESS_AVAILABLE", "false")
            buildConfigField("Boolean", "CAN_INSTALL_UPDATES", "false")
            buildConfigField("String", "UPDATE_ASSET_NAME", "\"foldstand-safe.apk\"")
        }
        create("full") {
            dimension = "distribution"
            buildConfigField("Boolean", "NOTIFICATION_ACCESS_AVAILABLE", "true")
            buildConfigField("Boolean", "CAN_INSTALL_UPDATES", "true")
            buildConfigField("String", "UPDATE_ASSET_NAME", "\"foldstand-full.apk\"")
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
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.window)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.datastore.preferences)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
