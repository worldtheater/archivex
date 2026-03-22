package com.worldtheater.archive.di

import com.worldtheater.archive.domain.repository.impl.LocalNoteRepositoryImpl
import com.worldtheater.archive.domain.repository.impl.SettingsRepositoryImpl
import com.worldtheater.archive.domain.repository.NoteRepository
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.debug.DebugDataGenerator
import com.worldtheater.archive.platform.auth.DefaultSensitiveAuthSessionStore
import com.worldtheater.archive.platform.auth.SensitiveAuthSessionStore
import com.worldtheater.archive.platform.gateway.DefaultExternalLinkOpener
import com.worldtheater.archive.platform.gateway.ExternalLinkOpener
import com.worldtheater.archive.platform.system.*
import org.koin.dsl.module

val commonAppModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl() }
    single<NoteRepository> { LocalNoteRepositoryImpl() }
    single<UserMessageSink> { DefaultUserMessageSink() }
    single<DateTimeFormatter> { DefaultDateTimeFormatter() }
    single<AppFeatureFlags> { DefaultAppFeatureFlags() }
    single<ExternalLinkOpener> { DefaultExternalLinkOpener() }
    single<SensitiveAuthSessionStore> { DefaultSensitiveAuthSessionStore() }
    single { DebugDataGenerator(get()) }
}
