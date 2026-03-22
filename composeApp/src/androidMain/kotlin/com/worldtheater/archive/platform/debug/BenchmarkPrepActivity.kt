package com.worldtheater.archive.platform.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BenchmarkPrepActivity : ComponentActivity() {

    private val benchmarkTestHooks: com.worldtheater.archive.platform.debug.BenchmarkTestHooks by inject()
    private var createdCount by mutableIntStateOf(0)
    private var totalCount by mutableIntStateOf(0)
    private var statusText by mutableStateOf("Preparing benchmark data...")
    private val prepBackdropColor = Color(0xB3000000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(prepBackdropColor),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                if (totalCount > 0) {
                    Text(
                        text = "$createdCount / $totalCount",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (!benchmarkTestHooks.isEnabled(this, intent)) {
            setErrorResult("benchmark_hooks_disabled")
            finish()
            return
        }

        val requestedCount =
            intent?.getIntExtra(_root_ide_package_.com.worldtheater.archive.platform.debug.BenchmarkTestHooks.EXTRA_BENCHMARK_SEED_COUNT, 500)
                ?: 500
        totalCount = requestedCount.coerceAtLeast(1)
        statusText = "Generating benchmark dataset..."
        lifecycleScope.launch {
            try {
                val created = benchmarkTestHooks.seedBenchmarkData(
                    totalCount = requestedCount.coerceAtLeast(1),
                    onProgress = { created, total ->
                        runOnUiThread {
                            createdCount = created
                            totalCount = total
                        }
                    }
                )
                statusText = "Benchmark dataset ready."
                setResult(
                    RESULT_OK,
                    Intent().putExtra(
                        BenchmarkTestHooks.EXTRA_BENCHMARK_SEED_CREATED_COUNT,
                        created
                    )
                )
            } catch (e: Exception) {
                setErrorResult(e.message ?: "unknown_error")
            } finally {
                finish()
            }
        }
    }

    private fun setErrorResult(message: String) {
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(BenchmarkTestHooks.EXTRA_BENCHMARK_SEED_ERROR, message)
        )
    }
}
