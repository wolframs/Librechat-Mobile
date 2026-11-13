package com.garfiec.librechat.fork.login.di

import com.garfiec.librechat.fork.login.PasswordManagerLoginBridge
import com.garfiec.librechat.fork.login.PendingCredentialSaveHandoff
import com.garfiec.librechat.fork.login.SavedLoginStore
import com.garfiec.librechat.fork.login.TwoFactorSaveBridge
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val savedLoginModule = module {
    singleOf(::SavedLoginStore)
    singleOf(::PendingCredentialSaveHandoff)
    viewModelOf(::PasswordManagerLoginBridge)
    viewModel { params -> TwoFactorSaveBridge(get(), params.getOrNull<String>(), get()) }
}
