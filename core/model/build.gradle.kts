plugins {
    id("librechat.kmp.library")
    id("librechat.kotlin.serialization")
}

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "com.garfiec.librechat.core.model"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
