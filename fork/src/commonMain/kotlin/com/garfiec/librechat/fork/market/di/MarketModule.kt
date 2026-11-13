package com.garfiec.librechat.fork.market.di

import com.garfiec.librechat.fork.market.MarketApi
import org.koin.dsl.module

val marketModule = module { single { MarketApi(get()) } }
