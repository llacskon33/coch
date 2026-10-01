package com.llacskon33.coch

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var updateState: ((CaptureUiState) -> Unit)? = null
    private var permissionRequestActive = false
    private var currentState = CaptureUiState()

    private val projectionPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (!permissionRequestActive) return@registerForActivityResult
        permissionRequestActive = false

        val permissionData = result.data
        if (result.resultCode != Activity.RESULT_OK || permissionData == null) {
            showState(currentState.permissionDenied())
            return@registerForActivityResult
        }

        showState(currentState.starting())
        try {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java)
                .setAction(ScreenCaptureService.ACTION_START)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, permissionData)
            startForegroundService(serviceIntent)
        } catch (exception: Exception) {
            showState(currentState.failed(exception.localizedMessage ?: "error del servicio"))
        }
    }

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = CaptureStatus.fromWireValue(
                intent.getStringExtra(ScreenCaptureService.EXTRA_STATUS)
            )
            val message = intent.getStringExtra(ScreenCaptureService.EXTRA_MESSAGE)
                ?: currentState.message
            showState(CaptureUiState(status, message))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequestActive = savedInstanceState?.getBoolean(KEY_PERMISSION_PENDING) ?: false
        currentState = savedInstanceState?.let {
            CaptureUiState(
                CaptureStatus.fromWireValue(it.getString(KEY_STATUS)),
                it.getString(KEY_MESSAGE) ?: "La captura está detenida."
            )
        } ?: CaptureUiState()

        val filter = IntentFilter(ScreenCaptureService.ACTION_STATE)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(serviceStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(serviceStateReceiver, filter)
        }

        setContent {
            var state by remember { mutableStateOf(currentState) }
            updateState = { next ->
                currentState = next
                state = next
            }

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Coch", style = MaterialTheme.typography.headlineMedium)
                    Text("Estado: ${state.status.name.lowercase().replace('_', ' ')}")
                    Text(state.message)

                    Button(
                        enabled = state.status !in setOf(
                            CaptureStatus.PERMISSION_PENDING,
                            CaptureStatus.STARTING,
                            CaptureStatus.RUNNING
                        ),
                        onClick = {
                            permissionRequestActive = true
                            showState(currentState.permissionPending())
                            try {
                                val manager = getSystemService(MEDIA_PROJECTION_SERVICE)
                                    as MediaProjectionManager
                                projectionPermission.launch(manager.createScreenCaptureIntent())
                            } catch (exception: Exception) {
                                permissionRequestActive = false
                                showState(
                                    currentState.failed(
                                        exception.localizedMessage ?: "no se pudo pedir permiso"
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Iniciar")
                    }

                    Button(
                        enabled = state.status in setOf(
                            CaptureStatus.PERMISSION_PENDING,
                            CaptureStatus.STARTING,
                            CaptureStatus.RUNNING
                        ),
                        onClick = {
                            permissionRequestActive = false
                            showState(currentState.stopped())
                            startService(
                                Intent(this@MainActivity, ScreenCaptureService::class.java)
                                    .setAction(ScreenCaptureService.ACTION_STOP)
                            )
                        }
                    ) {
                        Text("Parar")
                    }

                    if (state.status == CaptureStatus.RUNNING) {
                        Text("Sugerencias de ejemplo (no analizan la pantalla):")
                        SuggestionProvider.suggestions().forEach { suggestion ->
                            Text("• $suggestion")
                        }
                    }
                }
            }
        }
        sendBroadcast(
            Intent(ScreenCaptureService.ACTION_QUERY).setPackage(packageName)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PERMISSION_PENDING, permissionRequestActive)
        outState.putString(KEY_STATUS, currentState.status.wireValue)
        outState.putString(KEY_MESSAGE, currentState.message)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        updateState = null
        unregisterReceiver(serviceStateReceiver)
        super.onDestroy()
    }

    private fun showState(state: CaptureUiState) {
        currentState = state
        updateState?.invoke(state)
    }

    private companion object {
        const val KEY_PERMISSION_PENDING = "permission_pending"
        const val KEY_STATUS = "capture_status"
        const val KEY_MESSAGE = "capture_message"
    }
}
