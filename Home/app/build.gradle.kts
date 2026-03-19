plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.home"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.home"
        minSdk = 24
        targetSdk = 35
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
}

dependencies {
    testImplementation ("junit:junit:4.13.2'")
    testImplementation ("org.mockito:mockito-core:3.12.4")
    testImplementation ("org.json:json:20210307")


    testImplementation ("com.squareup.okhttp3:mockwebserver:4.11.0")

    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.google.android.material:material:1.11.0")


    implementation ("com.squareup.picasso:picasso:2.8")
    implementation ("com.jakewharton.picasso:picasso2-okhttp3-downloader:1.1.0")




    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}