group = "com.github.MFA-Projects"
version = "1.0.0"
//version = providers.gradleProperty("version").orElse("unspecified").get()
plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.mfa.admanager"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    publishing {
        singleVariant("release")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(libs.androidx.junit)

    api("com.google.android.gms:play-services-ads:25.5.0")

}