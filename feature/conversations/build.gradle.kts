plugins {
    id("librechat.kmp.feature")
}

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "com.garfiec.librechat.feature.conversations"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.paging.common)
            implementation(libs.kermit)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.paging.runtime)
            implementation(libs.paging.compose)
        }
        named("androidUnitTest").dependencies {
            implementation(libs.koin.test)
        }
    }
}
