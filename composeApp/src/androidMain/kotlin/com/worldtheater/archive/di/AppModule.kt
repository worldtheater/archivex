package com.worldtheater.archive.di

import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.feature.settings.screens.AboutScreenPlatformUiProvider
import com.worldtheater.archive.platform.auth.DeviceAuthAvailabilityChecker
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.auth.SensitiveAuthPrompt
import com.worldtheater.archive.platform.debug.BenchmarkTestHooks
import com.worldtheater.archive.platform.gateway.AndroidBackupDirectoryGateway
import com.worldtheater.archive.platform.gateway.AndroidBackupDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.AndroidClipboardGateway
import com.worldtheater.archive.platform.gateway.AndroidHapticFeedbackGateway
import com.worldtheater.archive.platform.gateway.AndroidImportExportDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.AndroidTransferFileGateway
import com.worldtheater.archive.platform.gateway.BackupDirectoryGateway
import com.worldtheater.archive.platform.gateway.BackupDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.ClipboardGateway
import com.worldtheater.archive.platform.gateway.HapticFeedbackGateway
import com.worldtheater.archive.platform.gateway.ImportExportDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.TransferFileGateway
import com.worldtheater.archive.platform.security.AndroidDeviceAuthAvailabilityChecker
import com.worldtheater.archive.platform.security.AndroidSensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.security.AndroidSensitiveAuthPrompt
import com.worldtheater.archive.platform.ui.DefaultAboutScreenPlatformUiProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(commonAppModule)

    single<AboutScreenPlatformUiProvider> { DefaultAboutScreenPlatformUiProvider() }
    single<TransferFileGateway> { AndroidTransferFileGateway(get()) }
    single<DeviceAuthAvailabilityChecker> { AndroidDeviceAuthAvailabilityChecker(get()) }
    single<SensitiveAuthAvailabilityProvider> { AndroidSensitiveAuthAvailabilityProvider(get()) }
    single<SensitiveAuthPrompt> { AndroidSensitiveAuthPrompt() }
    single<HapticFeedbackGateway> { AndroidHapticFeedbackGateway() }
    single<ClipboardGateway> { AndroidClipboardGateway() }
    single<BackupDirectoryGateway> { AndroidBackupDirectoryGateway() }
    single<BackupDocumentPickerGateway> { AndroidBackupDocumentPickerGateway() }
    single<ImportExportDocumentPickerGateway> { AndroidImportExportDocumentPickerGateway() }
    single { BenchmarkTestHooks(get(), get()) }
    viewModel { NoteListViewModel(get(), get(), get(), get(), get(), get()) }
}
