plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.apollo)
}

android {
    namespace = "in.aboobacker.labdroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "in.aboobacker.labdroid"
        minSdk = 29
        targetSdk = 37
        
        val gitVersionName = try {
            val process = Runtime.getRuntime().exec("git describe --tags --abbrev=0")
            process.inputStream.bufferedReader().readText().trim().removePrefix("v")
        } catch (e: Exception) {
            "1.0"
        }
        
        val gitVersionCode = try {
            val process = Runtime.getRuntime().exec("git rev-list --count HEAD")
            process.inputStream.bufferedReader().readText().trim().toInt()
        } catch (e: Exception) {
            1
        }

        versionCode = gitVersionCode
        versionName = gitVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "in.aboobacker.labdroid"

        buildConfigField("String", "GITLAB_CLIENT_ID", "\"${project.findProperty("GITLAB_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "GITLAB_BASE_URL", "\"${project.findProperty("GITLAB_BASE_URL") ?: "https://gitlab.com"}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "release.jks"
            if (file(keystorePath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fallback to debug signing so the APK is installable even if unsigned
                signingConfig = signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
}

apollo {
    service("service") {
        packageName.set("in.aboobacker.labdroid")
        introspection {
            endpointUrl.set("${project.findProperty("GITLAB_BASE_URL") ?: "https://gitlab.com"}/api/graphql")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.metadata.kotlin)
    annotationProcessor(libs.metadata.kotlin)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.network.okhttp)
    implementation(libs.appauth)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.normalized.cache)
    implementation(libs.apollo.normalized.cache.sqlite)
    implementation(libs.markdown)

    // Wear OS
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.work.runtime)
    debugImplementation(libs.androidx.wear.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}