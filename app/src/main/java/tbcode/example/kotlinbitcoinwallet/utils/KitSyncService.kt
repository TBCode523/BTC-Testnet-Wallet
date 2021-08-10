package tbcode.example.kotlinbitcoinwallet.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.kotlinbitcoinwallet.MainActivity
import tbcode.example.kotlinbitcoinwallet.R
import tbcode.example.kotlinbitcoinwallet.utils.builders.BTCKitBuilder
import java.util.*

class KitSyncService: LifecycleService(), BitcoinKit.Listener {
   private  var manager:NotificationManager? = null

    companion object {
       //  val walletId = "MyWallet"
       //  var networkType = BitcoinKit.NetworkType.TestNet
    //    var syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api()
     //    var bip = Bip.BIP84
        var progress = ""

        var kit = CryptoKits.BTC
        //Every 15 min.
        var syncTiming = 900000L
        var syncState = MutableLiveData<BitcoinCore.KitState>()
        var lastBlockInfo = MutableLiveData<BlockInfo>()
        lateinit var bitcoinKit: BitcoinKit
        lateinit var instance:KitSyncService
        var isRunning = false
        fun stopSync(){
            Log.d("btc-db", "Stopping Foreground Service!")
            isRunning = false
            bitcoinKit.stop()
            val alarmIntent = Intent(instance, KitBroadcastReceiver::class.java).let {
                    i -> PendingIntent.getBroadcast(instance, 0, i, 0)
            }
            val alarmMgr =
                instance.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmMgr?.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + syncTiming,
                alarmIntent
            )
            val date = Calendar.getInstance().time
            val newDate = Date(date.time + syncTiming)

            instance.stopSelf()
            instance.stopForeground(true)
            Log.d("btc-alert", "Service has stopped! Setting Alarm at ${newDate}!")
        }
    }
    init {
        instance = this
        isRunning = true
       
        
    }

    override fun onCreate() {
        super.onCreate()
        //TODO enhance this by using the crypto-enum class
        val sharedPref =  this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        val words = sharedPref.getString(BTCKitBuilder.walletId,null)?.split(" ")
        bitcoinKit = BTCKitBuilder.createKit(this, words!!)
        bitcoinKit.listener = this
        syncState.value = bitcoinKit.syncState
        lastBlockInfo.value = bitcoinKit.lastBlockInfo
        
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        manager = createNotificationChannel()

       val syncNotification = createSyncNoti("0")
        startForeground(1, syncNotification)
        syncKit()
        return START_STICKY
    }


    private fun syncKit() {
        //Determine the Kit
        Log.d("btc-db", "Starting the Sync...")
        var progress1 = ""

        syncState.observe(this, {
                state ->
            run {
               // Log.d("btc-service", "SyncMode: ${BTCKitBuilder.syncMode}")
                when (state) {
                    is BitcoinCore.KitState.Synced -> {
                        Log.d("btc-service", "Synced")
                        progress = "Synced!"
                        if(BTCKitBuilder.syncMode == BitcoinCore.SyncMode.NewWallet())
                        BTCKitBuilder.syncMode = BitcoinCore.SyncMode.Api()
                    if(!MainActivity.isActive){
                        Log.d("btc-service", "App is not active so Stopping Sync")

                        val intent = Intent(this, KitBroadcastReceiver::class.java)
                        this.sendBroadcast(intent)

                    }
                        else stopForeground(true)
                    }
                    is BitcoinCore.KitState.Syncing -> {

                        progress =
                            SpannableStringBuilder("%${"%.2f".format(state.progress * 100)}").toString()
                        //Log.d("btc-service", "${syncState.value}")
                       // Log.d("btc-service", progress)
                       // Log.d("btc-service", "${syncState.hasActiveObservers()}")
                        manager?.notify(1, createSyncNoti(progress))
                    }
                    is BitcoinCore.KitState.ApiSyncing -> {
                        Log.d("btc-service", "Api Syncing")
                        "api syncing ${state.transactions} txs"

                    }
                    is BitcoinCore.KitState.NotSynced -> {
                        Log.d("btc-service", "Wrecked")
                        "not synced ${state.exception.javaClass.simpleName}"
                    }
                }
            }
        })
        bitcoinKit.start()


    }
    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlockInfo.postValue(blockInfo)
    }
    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
       syncState.postValue(state)
    }

    private fun createNotificationChannel(): NotificationManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "ChannelID1",
                "Bitcoin Sync Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(notificationChannel)
            manager
        } else {
            null
        }
    }

    private fun createSyncNoti(str:String): Notification {
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0)
        return NotificationCompat.Builder(this, "ChannelID1")
            .setContentTitle("BTC Testnet")
            .setContentText("BitcoinKit is syncing: $str")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(false)
            .setNotificationSilent()
            .build()
    }
    fun getKit() = bitcoinKit
}