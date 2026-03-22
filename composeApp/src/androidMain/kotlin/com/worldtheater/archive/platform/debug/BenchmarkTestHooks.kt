package com.worldtheater.archive.platform.debug

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.worldtheater.archive.BuildConfig
import com.worldtheater.archive.domain.repository.SettingsRepository
import com.worldtheater.archive.feature.debug.DebugDataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BenchmarkTestHooks(
    private val settingsRepository: SettingsRepository,
    private val debugDataGenerator: DebugDataGenerator
) {

    fun shouldBypassSecureWindow(activity: Activity, intent: Intent?): Boolean {
        return isEnabled(activity, intent) &&
                intent?.getBooleanExtra(EXTRA_BENCHMARK_LAUNCH, false) == true
    }

    fun isEnabled(activity: Activity, intent: Intent?): Boolean {
        val isDebuggable =
            (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val isBenchmarkFlavor = BuildConfig.FLAVOR == "benchmark"
        val extraEnabled = intent?.getBooleanExtra(EXTRA_BENCHMARK_HOOKS_ENABLED, false) == true
        return isDebuggable && isBenchmarkFlavor && extraEnabled
    }

    suspend fun seedBenchmarkData(
        totalCount: Int,
        onProgress: (createdCount: Int, totalCount: Int) -> Unit
    ): Int {
        settingsRepository.setBiometricDisabled(true)
        _root_ide_package_.com.worldtheater.archive.platform.data.LocalNoteRepositoryPlatform.ensureDatabaseInitialized()
        return withContext(Dispatchers.IO) {
            _root_ide_package_.com.worldtheater.archive.platform.data.LocalNoteRepositoryPlatform.noteDao().deleteAll()
            val result = debugDataGenerator.generateBenchmarkDataset(
                totalCount = totalCount,
                onProgress = onProgress
            )
            result.limitException?.let {
                throw IllegalStateException(
                    it.message ?: "limit_reached"
                )
            }
            result.createdCount
        }
    }

    companion object {
        const val EXTRA_BENCHMARK_HOOKS_ENABLED = "extra_benchmark_hooks_enabled"
        const val EXTRA_BENCHMARK_LAUNCH = "extra_benchmark_launch"
        const val EXTRA_BENCHMARK_SEED_COUNT = "extra_benchmark_seed_count"
        const val EXTRA_BENCHMARK_SEED_CREATED_COUNT = "extra_benchmark_seed_created_count"
        const val EXTRA_BENCHMARK_SEED_ERROR = "extra_benchmark_seed_error"
    }
}
