package com.example.oylfrutossecos.ui.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.oylfrutossecos.MainActivity
import com.example.oylfrutossecos.data.model.AppNotice
import com.example.oylfrutossecos.ui.components.noticeTitle
import com.example.oylfrutossecos.ui.components.noticeMessage

/** Demostración de aviso LOCAL. Las push remotas requieren un servicio y backend. */
object DemoLocalNotifier {
    private const val CHANNEL = "oyl_demo_notifications"

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, notice: AppNotice): Boolean {
        if (!hasPermission(context)) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "Avisos de OYL", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos de pedidos y productos en este dispositivo" })
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pending = PendingIntent.getActivity(context, 0, intent, flags)
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(context, CHANNEL) else Notification.Builder(context)
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(noticeTitle(notice))
            .setContentText(noticeMessage(notice))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(notice.id.toInt(), notification)
            true
        } catch (_: SecurityException) { false }
    }
}
