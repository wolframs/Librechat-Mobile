package com.garfiec.librechat.fork.di

import org.koin.core.module.Module
import org.koin.dsl.module

@Suppress("EmptyModule") // The expect/actual platform seam remains available when a platform adds bindings.
actual val forkPlatformModule: Module = module { }
