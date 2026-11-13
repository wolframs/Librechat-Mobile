package com.garfiec.librechat.fork.di

import org.koin.core.module.Module
import org.koin.dsl.module

expect val forkPlatformModule: Module

/** Everything the fork adds to the Koin graph. Listed once in `sharedKoinModules`. */
val forkModule = module {
    includes(forkPlatformModule)
    includes(com.garfiec.librechat.fork.cachettl.di.cacheTtlModule)
    includes(com.garfiec.librechat.fork.market.di.marketModule)
}
