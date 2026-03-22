package com.worldtheater.archive.di

import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.feature.settings.screens.AboutScreenPlatformUiProvider
import com.worldtheater.archive.platform.auth.DesktopDeviceAuthAvailabilityChecker
import com.worldtheater.archive.platform.auth.DesktopSensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.auth.DesktopSensitiveAuthPrompt
import com.worldtheater.archive.platform.auth.DeviceAuthAvailabilityChecker
import com.worldtheater.archive.platform.auth.SensitiveAuthAvailabilityProvider
import com.worldtheater.archive.platform.auth.SensitiveAuthPrompt
import com.worldtheater.archive.platform.gateway.BackupDirectoryGateway
import com.worldtheater.archive.platform.gateway.BackupDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.ClipboardGateway
import com.worldtheater.archive.platform.gateway.DesktopBackupDirectoryGateway
import com.worldtheater.archive.platform.gateway.DesktopBackupDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.DesktopClipboardGateway
import com.worldtheater.archive.platform.gateway.DesktopHapticFeedbackGateway
import com.worldtheater.archive.platform.gateway.DesktopImportExportDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.DesktopTransferFileGateway
import com.worldtheater.archive.platform.gateway.HapticFeedbackGateway
import com.worldtheater.archive.platform.gateway.ImportExportDocumentPickerGateway
import com.worldtheater.archive.platform.gateway.TransferFileGateway
import com.worldtheater.archive.platform.ui.DefaultAboutScreenPlatformUiProvider
import org.koin.dsl.module

val appModule = module {
    includes(commonAppModule)

    single<AboutScreenPlatformUiProvider> { DefaultAboutScreenPlatformUiProvider() }
    single<TransferFileGateway> { DesktopTransferFileGateway }
    single<DeviceAuthAvailabilityChecker> { DesktopDeviceAuthAvailabilityChecker }
    single<SensitiveAuthAvailabilityProvider> { DesktopSensitiveAuthAvailabilityProvider }
    single<SensitiveAuthPrompt> { DesktopSensitiveAuthPrompt }
    single<HapticFeedbackGateway> { DesktopHapticFeedbackGateway }
    single<ClipboardGateway> { DesktopClipboardGateway }
    single<BackupDirectoryGateway> { DesktopBackupDirectoryGateway }
    single<BackupDocumentPickerGateway> { DesktopBackupDocumentPickerGateway() }
    single<ImportExportDocumentPickerGateway> { DesktopImportExportDocumentPickerGateway() }
    single { NoteListViewModel(get(), get(), get(), get(), get(), get()) }
}
