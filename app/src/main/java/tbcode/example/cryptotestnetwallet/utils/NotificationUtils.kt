package tbcode.example.cryptotestnetwallet.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import tbcode.example.cryptotestnetwallet.MainActivity
import tbcode.example.cryptotestnetwallet.R

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
     fun createSyncNotification(progressStr:String, blockStr:String, context: Context): Notification {
        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, channelID)
            .setContentTitle("BTC Testnet")
            .setContentText(progressStr)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(progressStr+blockStr))
            .setOnlyAlertOnce(false)
            .setNotificationSilent()
            .build()
    }
    fun createBaseNotification(notiStr:String, context: Context): Notification{
        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, channelID)
            .setContentTitle("BTC Testnet")
            .setContentText(notiStr)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(false)
            .setNotificationSilent()
            .build()
    }
    fun createBalanceNotification(balanceStr:String, context: Context): Notification{
        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, channelID)
            .setContentTitle("BTC Testnet")
            .setContentText("Transaction Confirmed!")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(balanceStr))
            .setOnlyAlertOnce(false)
            .setNotificationSilent()
            .build()
    }


}