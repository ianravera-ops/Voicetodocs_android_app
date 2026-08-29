package com.voicetodocs.cos.data.digest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.voicetodocs.cos.MainActivity
import com.voicetodocs.cos.R
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.LocaleHelper
import com.voicetodocs.cos.data.VipDigestItem

class VipNotifier(private val context: Context) {
    fun show(items: List<VipDigestItem>, language: AppLanguage) {
        if (items.isEmpty()) return
        val loc = LocaleHelper.wrap(context, language)
        ensureChannel(loc)
        val title = loc.getString(R.string.vip_notify_title)
        val body = items.joinToString("\n") { item ->
            val who = item.from.ifBlank { item.subject }
            "${who}: ${item.summary}"
        }.take(400)
        val launch = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(launch)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, notification)
        } catch (_: SecurityException) {
            // Permission not granted; Home still shows the digest.
        }
    }

    private fun ensureChannel(loc: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                loc.getString(R.string.vip_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = loc.getString(R.string.vip_channel_desc)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "vip_digest"
        const val NOTIFY_ID = 41
    }
}
