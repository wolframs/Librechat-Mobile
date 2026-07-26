plugins {
    id("librechat.kmp.feature")
}

android {
    namespace = "com.garfiec.librechat.feature.auth"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
        }
        androidMain.dependencies {
            implementation(libs.browser)
            implementation(libs.credentials)
            implementation(libs.credentials.play.services.auth)
        }
        named("androidUnitTest").dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.test)
        }
    }
}
