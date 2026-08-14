package com.noorequran.app

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer") ?: "Prayer"
        val uriText = intent.getStringExtra("adhan_uri") ?: ""
        val sound = if (uriText.isNotBlank()) Uri.parse(uriText) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channelId = "prayer_${prayer.lowercase()}"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "$prayer Adhan", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Noor-e-Quran $prayer notification"
            ch.setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            nm.createNotificationChannel(ch)
        }
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channelId) else Notification.Builder(context)
        b.setSmallIcon(com.noorequran.app.R.drawable.ic_noor_quran)
            .setContentTitle("Noor-e-Quran")
            .setContentText("$prayer time")
            .setAutoCancel(true)
        if (Build.VERSION.SDK_INT < 26) b.setSound(sound)
        nm.notify(prayer.hashCode(), b.build())
    }
}
