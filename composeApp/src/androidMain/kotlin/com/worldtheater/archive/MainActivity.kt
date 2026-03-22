package com.worldtheater.archive

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.note_list.screens.NoteListViewModel
import com.worldtheater.archive.nav.AppComposeNavigatorImpl
import com.worldtheater.archive.nav.base.AppComposeNavigator
import com.worldtheater.archive.platform.security.*
import com.worldtheater.archive.platform.system.applyMainWindowPolicy
import com.worldtheater.archive.platform.system.applySecureWindowPolicy
import com.worldtheater.archive.platform.system.disableNewContextMenuCompat
import com.worldtheater.archive.security.AppForegroundAuthCoordinator
import com.worldtheater.archive.security.disableBiometricAuthAndProceed
import com.worldtheater.archive.security.shouldSkipBiometricForEmptyData
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private val noteListVM by viewModel<NoteListViewModel>()
    private val settingsRepository: SettingsRepository by inject()
    private val benchmarkTestHooks: com.worldtheater.archive.platform.debug.BenchmarkTestHooks by inject()
    private val onUnlockSuccess: () -> Unit = {
        foregroundAuthCoordinator.markAuthenticatedSession()
        initSecureDbAndLoadNotes(tag = TAG, noteListViewModel = noteListVM)
    }

    private val appComposeNavigator: AppComposeNavigator = AppComposeNavigatorImpl()
    private val foregroundAuthCoordinator by lazy {
        AppForegroundAuthCoordinator(
            settingsRepository = settingsRepository,
            noteListViewModel = noteListVM,
            shouldSkipBiometricForEmptyData = {
                shouldSkipBiometricForEmptyData(
                    tag = TAG,
                    settingsRepository = settingsRepository
                )
            },
            onBiometricRequired = {
                requestAppUnlock(
                    tag = TAG,
                    settingsRepository = settingsRepository,
                    onIssue = { showBiometricIssueDialog = true },
                    onSuccess = onUnlockSuccess
                )
            },
            onBiometricSuccess = onUnlockSuccess
        )
    }

    private var showBiometricIssueDialog by mutableStateOf(false)

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isBenchmarkLaunch = benchmarkTestHooks.shouldBypassSecureWindow(this, intent)
        applyMainWindowPolicy(
            allowScreenshots = settingsRepository.isAllowScreenshotsEnabled(),
            isBenchmarkLaunch = isBenchmarkLaunch
        )
        lifecycleScope.launch {
            settingsRepository.allowScreenshotsFlow.collect { allowScreenshots ->
                applySecureWindowPolicy(
                    allowScreenshots = allowScreenshots,
                    isBenchmarkLaunch = isBenchmarkLaunch
                )
            }
        }

        disableNewContextMenuCompat()

        setContent {
            MainActivityContent(
                settingsRepository = settingsRepository,
                appComposeNavigator = appComposeNavigator,
                noteListViewModel = noteListVM,
                showBiometricIssueDialog = showBiometricIssueDialog,
                onGoSettings = {
                    showBiometricIssueDialog = false
                    this.handleBiometricIssueGoSettings()
                },
                onDisableBiometric = {
                    showBiometricIssueDialog = false
                    lifecycleScope.launch {
                        disableBiometricAuthAndProceed(
                            settingsRepository = settingsRepository,
                            onBiometricSuccess = onUnlockSuccess
                        )
                    }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        AndroidSensitiveAuthPrompt.attachActivity(this)
        lifecycleScope.launch { foregroundAuthCoordinator.onStart() }
    }

    override fun onStop() {
        super.onStop()
        AndroidSensitiveAuthPrompt.detachActivity(this)
        foregroundAuthCoordinator.onStop()
    }

}
