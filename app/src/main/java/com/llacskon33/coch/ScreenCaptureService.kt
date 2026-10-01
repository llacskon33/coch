package com.llacskon33.coch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var currentStatus = CaptureStatus.IDLE
    private var currentMessage = "La captura está detenida."

    private val queryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_QUERY) {
                broadcastState(currentStatus, currentMessage)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_QUERY)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(queryReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(queryReceiver, filter)
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture(intent, startId)
            ACTION_STOP -> {
                releaseCapture(stopProjection = true)
                updateState(CaptureStatus.STOPPED, "La captura está detenida.")
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startCapture(intent: Intent, startId: Int) {
        if (projection != null) {
            updateState(
                CaptureStatus.RUNNING,
                "Captura activa. Las sugerencias son solo ejemplos fijos."
            )
            return
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultData == null) {
            updateState(CaptureStatus.ERROR, "Falta el permiso de captura de Android.")
            stopSelf(startId)
            return
        }

        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = manager.getMediaProjection(resultCode, resultData)
            projection = mediaProjection
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    releaseCapture(stopProjection = false)
                    updateState(CaptureStatus.STOPPED, "La captura fue detenida por Android.")
                    stopSelf()
                }
            }
            projectionCallback = callback
            mediaProjection.registerCallback(callback, Handler(Looper.getMainLooper()))

            val metrics = resources.displayMetrics
            val reader = ImageReader.newInstance(
                metrics.widthPixels,
                metrics.heightPixels,
                android.graphics.PixelFormat.RGBA_8888,
                2
            )
            imageReader = reader
            reader.setOnImageAvailableListener(
                { source -> source.acquireLatestImage()?.close() },
                Handler(Looper.getMainLooper())
            )
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "CochScreenCapture",
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            ) ?: throw IllegalStateException("Android no creó la pantalla virtual.")
            updateState(
                CaptureStatus.RUNNING,
                "Captura activa. Las sugerencias son solo ejemplos fijos."
            )
        } catch (exception: Exception) {
            releaseCapture(stopProjection = true)
            stopForeground(STOP_FOREGROUND_REMOVE)
            updateState(
                CaptureStatus.ERROR,
                "No se pudo iniciar la captura: ${exception.localizedMessage ?: "error del servicio"}"
            )
            stopSelf(startId)
        }
    }

    private fun releaseCapture(stopProjection: Boolean) {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null

        val activeProjection = projection
        val callback = projectionCallback
        if (activeProjection != null && callback != null) {
            activeProjection.unregisterCallback(callback)
        }
        projectionCallback = null
        projection = null
        if (stopProjection) {
            activeProjection?.stop()
        }
    }

    private fun updateState(status: CaptureStatus, message: String) {
        currentStatus = status
        currentMessage = message
        broadcastState(status, message)
    }

    private fun broadcastState(status: CaptureStatus, message: String) {
        sendBroadcast(
            Intent(ACTION_STATE)
                .setPackage(packageName)
                .putExtra(EXTRA_STATUS, status.wireValue)
                .putExtra(EXTRA_MESSAGE, message)
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Captura de pantalla",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Coch está capturando la pantalla")
                .setContentText("Las sugerencias son ejemplos fijos.")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Coch está capturando la pantalla")
                .setContentText("Las sugerencias son ejemplos fijos.")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        releaseCapture(stopProjection = true)
        updateState(CaptureStatus.STOPPED, "La captura está detenida.")
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        releaseCapture(stopProjection = true)
        unregisterReceiver(queryReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.llacskon33.coch.action.START"
        const val ACTION_STOP = "com.llacskon33.coch.action.STOP"
        const val ACTION_QUERY = "com.llacskon33.coch.action.QUERY"
        const val ACTION_STATE = "com.llacskon33.coch.action.STATE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_STATUS = "status"
        const val EXTRA_MESSAGE = "message"

        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 1
    }
}
