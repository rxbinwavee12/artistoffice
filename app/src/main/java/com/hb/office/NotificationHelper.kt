package com.hb.office

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "artist_office_channel"
    private const val CHANNEL_NAME = "Artist Office Benachrichtigungen"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kanal für wichtige Updates wie Konto-Freischaltungen"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showApprovalNotification(context: Context) {
        createNotificationChannel(context)

        // In-App Toast Hinweis anzeigen
        Toast.makeText(context, "🎉 Account Freigeschaltet! Du hast jetzt Zugriff auf deine Projekte.", Toast.LENGTH_LONG).show()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Account Freigeschaltet! 🎉")
            .setContentText("Dein Account wurde freigeschaltet. Du kannst jetzt auf deine Projekte zugreifen.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        try {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                with(NotificationManagerCompat.from(context)) {
                    notify(1001, builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
