package com.garfiec.librechat.fork.cachettl.di

import com.garfiec.librechat.core.data.fork.cachettl.CacheTtlMarkerStore
import org.koin.dsl.module

val cacheTtlModule = module { single { CacheTtlMarkerStore(get()) } }
