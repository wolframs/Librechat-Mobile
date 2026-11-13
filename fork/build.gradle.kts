// The fork's own module. Every feature module depends on it through the feature convention plugin,
// so it cannot apply that plugin itself; it takes the same pieces one by one.
plugins {
    id("librechat.kmp.library")
    id("librechat.kmp.compose")
    id("librechat.kmp.koin")
    id("librechat.kotlin.serialization")
}

android {
    namespace = "com.garfiec.librechat.fork"
}

// A `fork` package inside a feature module reads these strings and icons.
compose.resources {
    publicResClass = true
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ui"))
            implementation(project(":core:data"))
            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(project(":core:network"))
            implementation(project(":core:logging"))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.runtime.compose.kmp)
            implementation(libs.lifecycle.viewmodel.compose.kmp)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
            implementation(libs.datastore.preferences)
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.fork.credentials)
            implementation(libs.fork.credentials.play.services.auth)
            implementation(libs.koin.android)
        }
        getByName("androidUnitTest").dependencies {
            implementation(libs.koin.test)
        }
    }
}
