package com.worldtheater.archive.di

import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.feature.settings.screens.AboutScreenPlatformUiProvider
import com.worldtheater.archive.platform.auth.DeviceAuthAvailabilityChecker
import com.worldtheater.archive.platform.auth.IosDeviceAuthAvailabilityChecker
import com.worldtheater.archive.platform.auth.IosSensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.auth.IosSensitiveAuthPrompt
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.auth.SensitiveAuthPrompt
import com.worldtheater.archive.platform.gateway.BackupDirectoryGateway
import com.worldtheater.archive.platform.gateway.BackupDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.ClipboardGateway
import com.worldtheater.archive.platform.gateway.HapticFeedbackGateway
import com.worldtheater.archive.platform.gateway.ImportExportDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.IosBackupDirectoryGateway
import com.worldtheater.archive.platform.gateway.IosBackupDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.IosClipboardGateway
import com.worldtheater.archive.platform.gateway.IosHapticFeedbackGateway
import com.worldtheater.archive.platform.gateway.IosImportExportDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.IosTransferFileGateway
import com.worldtheater.archive.platform.gateway.TransferFileGateway
import com.worldtheater.archive.platform.ui.DefaultAboutScreenPlatformUiProvider
import org.koin.dsl.module

val appModule = module {
    includes(commonAppModule)

    single<AboutScreenPlatformUiProvider> { DefaultAboutScreenPlatformUiProvider() }
    single<TransferFileGateway> { IosTransferFileGateway }
    single<DeviceAuthAvailabilityChecker> { IosDeviceAuthAvailabilityChecker }
    single<SensitiveAuthAvailabilityProvider> { IosSensitiveAuthAvailabilityProvider }
    single<SensitiveAuthPrompt> { IosSensitiveAuthPrompt }
    single<HapticFeedbackGateway> { IosHapticFeedbackGateway }
    single<ClipboardGateway> { IosClipboardGateway }
    single<BackupDirectoryGateway> { IosBackupDirectoryGateway }
    single<BackupDocumentPickerGateway> { IosBackupDocumentPickerGateway() }
    single<ImportExportDocumentPickerGateway> { IosImportExportDocumentPickerGateway() }
    single { NoteListViewModel(get(), get(), get(), get(), get(), get()) }
}
