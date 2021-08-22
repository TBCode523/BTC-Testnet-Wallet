package tbcode.example.kotlinbitcoinwallet.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import tbcode.example.kotlinbitcoinwallet.MainActivity
import tbcode.example.kotlinbitcoinwallet.R

object NotificationUtils {
    private const val channelID = "ChannelID1"
    private const val channelName = "Testnet Notification"
     fun createNotificationChannel(context: Context): NotificationManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
               channelID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(notificationChannel)
            manager
        } else {
            null
        }
    }
     fun createSyncNoti(str:String, context: Context): Notification {
        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, 0)
        return NotificationCompat.Builder(context, channelID)
            .setContentTitle("BTC Testnet")
            .setContentText("BitcoinKit is syncing: $str")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(false)
            .setNotificationSilent()
            .build()
    }
}