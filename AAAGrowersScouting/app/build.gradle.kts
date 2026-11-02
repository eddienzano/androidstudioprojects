plugins {
    alias(libs.plugins.android.application)
}

android {

    namespace = "com.aaagrowers.scoutingapp"

    compileSdk = 36

    defaultConfig {

        applicationId =
            "com.aaagrowers.scoutingapp"

        minSdk = 24

        targetSdk = 36

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }


    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }
}


dependencies {

    /*
    |--------------------------------------------------------------------------
    | Android
    |--------------------------------------------------------------------------
    */

    implementation(
        libs.androidx.core.ktx
    )


    implementation(
        libs.androidx.appcompat
    )


    /*
    |--------------------------------------------------------------------------
    | Lifecycle
    |--------------------------------------------------------------------------
    */

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )


    /*
    |--------------------------------------------------------------------------
    | Room Database
    |--------------------------------------------------------------------------
    */

    implementation(
        libs.androidx.room.runtime
    )


    implementation(
        libs.androidx.room.ktx
    )


    annotationProcessor(
        libs.androidx.room.compiler
    )


    /*
    |--------------------------------------------------------------------------
    | Testing
    |--------------------------------------------------------------------------
    */

    testImplementation(
        libs.junit
    )


    androidTestImplementation(
        libs.androidx.junit
    )


    androidTestImplementation(
        libs.androidx.espresso.core
    )
    implementation("com.google.code.gson:gson:2.11.0")

    // QR / Barcode scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
}