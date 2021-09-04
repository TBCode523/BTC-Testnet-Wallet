package tbcode.example.kotlinbitcoinwallet.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.kotlinbitcoinwallet.MainActivity
import tbcode.example.kotlinbitcoinwallet.NumberFormatHelper
import tbcode.example.kotlinbitcoinwallet.utils.builders.BTCKitBuilder
import java.text.SimpleDateFormat
import java.util.*

class KitSyncService: LifecycleService(), BitcoinKit.Listener {
   private  var manager:NotificationManager? = null
    private val syncId = 1
    companion object {

        var progress = ""
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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
            if(BTCKitBuilder.syncMode == BitcoinCore.SyncMode.NewWallet())
                BTCKitBuilder.syncMode = BitcoinCore.SyncMode.Api()
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
        val sharedPref =  this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        val words = "burst detect lawsuit monitor royal sad guilt dwarf fold notable embark theme".split(" ")
        bitcoinKit = BTCKitBuilder.createKit(this, words)
        bitcoinKit.listener = this
        syncState.value = bitcoinKit.syncState
        lastBlockInfo.value = bitcoinKit.lastBlockInfo
        
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        manager = NotificationUtils.createNotificationChannel(this)

       val syncNotification = NotificationUtils.createNotification("Starting to sync", this)
        startForeground(syncId, syncNotification)
        syncKit()
        return START_STICKY
    }


    private fun syncKit() {
        //Determine the Kit
        Log.d("btc-db", "Starting the Sync...")

        syncState.observe(this, {
                state ->
            run {
               // Log.d("btc-service", "SyncMode: ${BTCKitBuilder.syncMode}")
                when (state) {
                    is BitcoinCore.KitState.Synced -> {
                        Log.d("btc-service", "Synced")
                        progress = "Synced!"

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
                        lastBlockInfo.observe(this, {
                            it?.let {
                                    blockInfo ->  progress +=SpannableStringBuilder("Block-Date: ${dateFormat.format(Date(blockInfo.timestamp * 1000))}\n").toString()
                                progress += SpannableStringBuilder("Block-Height: ${blockInfo.height}\n").toString()
                            }
                            manager?.notify(1, NotificationUtils.createNotification("Syncing with the Blockchain:$progress", this))

                        })
                      //  progress += lastBlockInfo.value
                        //Log.d("btc-service", "${syncState.value}")
                       // Log.d("btc-service", progress)
                       // Log.d("btc-service", "${syncState.hasActiveObservers()}")
                       // manager?.notify(1, NotificationUtils.createNotification("Syncing with the Blockchain:$progress", this))
                    }
                    is BitcoinCore.KitState.ApiSyncing -> {
                        Log.d("btc-service", "Api Syncing")
                        "api syncing ${state.transactions} txs"

                    }
                    is BitcoinCore.KitState.NotSynced -> {
                       // Log.d("btc-service", "Wrecked")
                        progress = "Unable to sync!"
                        manager?.notify(1, NotificationUtils.createNotification("Sync was interrupted!", this))
                       // Toast.makeText(this, "Unable to Sync!", Toast.LENGTH_SHORT).show()
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

    override fun onBalanceUpdate(balance: BalanceInfo) {
        super.onBalanceUpdate(balance)
        val newBalanceStr = "${NumberFormatHelper.cryptoAmountFormat.format(balance.spendable / 100_000_000.0)} tBTC"
        manager?.notify(2,NotificationUtils.createNotification("New Balance: $newBalanceStr", this))
    }



}