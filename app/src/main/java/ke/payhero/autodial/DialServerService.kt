package ke.payhero.autodial

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class DialServerService : Service() {

    companion object {
        private const val CHANNEL_ID = "autodial_api"
        private const val NOTIFICATION_ID = 17

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DialServerService::class.java)
            )
        }
    }

    private var server: LocalDialServer? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startInForeground()

        server = LocalDialServer(DialApiState.PORT) { number ->
            Dialer.place(this, number)
        }

        try {
            server?.start()
        } catch (_: Exception) {
            DialApiState.running = false
            DialApiState.lastMessage = "Could not bind port ${DialApiState.PORT}"
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_dial)
            .setContentTitle("AutoDial API")
            .setContentText("Listening on port ${DialApiState.PORT}")
            .setContentIntent(launch)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Local dial API",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the AutoDial HTTP API available"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
