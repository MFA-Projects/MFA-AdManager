plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}
group = "com.github.MFA-Projects.MFA-AdManager"
version = "1.0.3"

android {
    namespace = "com.mfa.admanager.unity"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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

                groupId = "com.github.MFA-Projects.MFA-AdManager"
                artifactId = "Unity"
                version = "1.0.3"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    api("com.unity3d.ads:unity-ads:4.22.0")
}