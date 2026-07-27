plugins {
    id("librechat.kmp.feature")
}

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "com.garfiec.librechat.feature.files"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
            implementation(libs.ktor.client.core)
            implementation(libs.kermit)
            implementation(libs.coil3.compose)
        }
        named("androidUnitTest").dependencies {
            implementation(libs.koin.test)
        }
    }
}
