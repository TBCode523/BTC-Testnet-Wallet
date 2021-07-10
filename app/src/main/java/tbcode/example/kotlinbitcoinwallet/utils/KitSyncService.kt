package tbcode.example.kotlinbitcoinwallet.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.kotlinbitcoinwallet.MainActivity
import tbcode.example.kotlinbitcoinwallet.R
import java.util.*

class KitSyncService: Service(), BitcoinKit.Listener {
    companion object {

        private val walletId = "MyWallet"
        private var networkType = BitcoinKit.NetworkType.TestNet
        private var syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api()
        private var bip = Bip.BIP84
        var kitState = MutableLiveData<BitcoinCore.KitState>()
        var lastBlock = MutableLiveData<BlockInfo>()
        private lateinit var bitcoinKit: BitcoinKit
        lateinit var instance:KitSyncService
        var isRunning = false
        fun stopSync(){
            Log.d("btc-db", "Stopping Foreground Service!")
            isRunning = false
            bitcoinKit.stop()
            val alarmIntent = Intent(instance, KitBroadcastReceiver::class.java).let {
                    i -> PendingIntent.getBroadcast(instance, 0, i, 0)
            }
            //  val alarmPending = PendingIntent.getActivity(this, 1, alarmIntent, 0)
            val alarmMgr =
                instance.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmMgr?.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 30000L,
                alarmIntent
            )
            val date = Calendar.getInstance().time
            val newDate = Date(date.time + 30000L)
            Log.d("btc-db", "Setting Alarm at ${newDate}!")
            instance.stopSelf()
            instance.stopForeground(true)

        }
    }
    init {
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent,0)
        val notification = NotificationCompat.Builder(this, "ChannelID1")
            .setContentTitle("BTC Testnet")
            .setContentText("BitcoinKit is syncing %${"%.2f".format((1 * 100).toFloat())}")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        syncKit()
        return START_STICKY
    }

    private fun syncKit() {
        //Determine the Kit
        Log.d("btc-db", "Stopping Foreground Service from Receiver")
       val sharedPref =  this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        val words = sharedPref.getString(walletId,null)?.split(" ")
        bitcoinKit = BitcoinKit(this,words!!, walletId, networkType, syncMode = syncMode, bip = bip)

        bitcoinKit.start()
//TODO Check if app is running or not if(!running)
    }
    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlock.postValue(blockInfo)
    }
    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
       kitState.postValue(state)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "ChannelID1",
                "Bitcoin Sync Notification",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    fun getKit() = bitcoinKit
}