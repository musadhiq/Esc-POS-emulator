package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.repository.PrintJobRepository
import com.example.server.HttpPrintServer
import com.example.server.NetworkHelper
import com.example.server.TcpPrintServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PrinterServerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var tcpServer: TcpPrintServer
    private lateinit var httpServer: HttpPrintServer
    private lateinit var repository: PrintJobRepository

    override fun onCreate() {
        super.onCreate()
        repository = PrintJobRepository.getInstance(applicationContext)
        tcpServer = TcpPrintServer(repository, serviceScope)
        httpServer = HttpPrintServer(repository, serviceScope)

        instance = this
        activeTcpServer = tcpServer
        activeHttpServer = httpServer

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopServers()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val tcpPort = intent?.getIntExtra(EXTRA_TCP_PORT, 9100) ?: 9100
        val httpPort = intent?.getIntExtra(EXTRA_HTTP_PORT, 9101) ?: 9101

        startForeground(NOTIFICATION_ID, createNotification(tcpPort, httpPort))

        tcpServer.start(tcpPort)
        httpServer.start(httpPort)

        return START_STICKY
    }

    private fun stopServers() {
        tcpServer.stop()
        httpServer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServers()
        serviceScope.cancel()
        instance = null
        activeTcpServer = null
        activeHttpServer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ESC/POS Printer Emulator",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running TCP & HTTP print listening servers"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(tcpPort: Int, httpPort: Int): Notification {
        val ip = NetworkHelper.getLocalIpAddress(this)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, PrinterServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ESC/POS Printer Emulator Active")
            .setContentText("Listening on $ip:TCP $tcpPort / HTTP $httpPort")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "printer_server_channel"
        const val NOTIFICATION_ID = 101
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val EXTRA_TCP_PORT = "EXTRA_TCP_PORT"
        const val EXTRA_HTTP_PORT = "EXTRA_HTTP_PORT"

        var instance: PrinterServerService? = null
            private set
        var activeTcpServer: TcpPrintServer? = null
            private set
        var activeHttpServer: HttpPrintServer? = null
            private set

        fun startService(context: Context, tcpPort: Int = 9100, httpPort: Int = 9101) {
            val intent = Intent(context, PrinterServerService::class.java).apply {
                putExtra(EXTRA_TCP_PORT, tcpPort)
                putExtra(EXTRA_HTTP_PORT, httpPort)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PrinterServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
