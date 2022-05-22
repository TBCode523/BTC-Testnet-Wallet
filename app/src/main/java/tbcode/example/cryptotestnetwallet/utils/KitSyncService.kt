package tbcode.example.cryptotestnetwallet.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.cryptotestnetwallet.MainActivity
import tbcode.example.cryptotestnetwallet.NumberFormatHelper
import tbcode.example.cryptotestnetwallet.utils.kit_builders.BTCKitBuilder
import java.text.SimpleDateFormat
import java.util.*

class KitSyncService: LifecycleService(), BitcoinKit.Listener {
   private  var manager:NotificationManager? = null
    private val syncId = 1
    var progress = ""
    companion object {


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
            instance.stopForeground(true)
            isRunning = false
            bitcoinKit.stop()


            val alarmIntent = Intent(instance, KitBroadcastReceiver::class.java).let {
                    i -> PendingIntent.getBroadcast(instance, 0, i, 0)
            }

            val alarmMgr =
                instance.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if(alarmMgr?.nextAlarmClock == null) {
                alarmMgr?.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + syncTiming,
                    alarmIntent
                )
                val date = Calendar.getInstance().time
                val newDate = Date(date.time + syncTiming)
                Toast.makeText(
                    this.instance,
                    "Next Sync will occur at: $newDate",
                    Toast.LENGTH_LONG
                ).show()

            }
            instance.stopSelf()
            Log.d("btc-alert", "Kit has stopped! Setting Alarm at ${alarmMgr?.nextAlarmClock?.triggerTime?.let {
                Date(
                    it
                )
            }}!")


        }
    }
    init {
        instance = this
        isRunning = true

        
    }

    override fun onCreate() {
        super.onCreate()
        //  val sharedPref =  this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        Log.d("btc-service", "Kit Builder syncMode: ${BTCKitBuilder.syncMode}")
        val sharedPref = this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)

       // var words = "consider final nuclear useless man come layer much tray pull rough dry".split(" ")
        val words = sharedPref.getString(BTCKitBuilder.walletId,null)?.split(" ")
        Log.d("btc-service", "Kit Builder words: $words")
        bitcoinKit = BTCKitBuilder.createKit(this, words!!)
        bitcoinKit.listener = this
        syncState.value = bitcoinKit.syncState
        lastBlockInfo.value = bitcoinKit.lastBlockInfo

        
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        manager = NotificationUtils.createNotificationChannel(this)

       val syncNotification = NotificationUtils.createBaseNotification("Starting to Sync!", this)
        startForeground(syncId, syncNotification)
        syncKit()
        return START_STICKY
    }



    private fun syncKit() {
        //Determine the Kit
        Log.d("btc-service", "Starting the Sync...")
    try {


        syncState.observe(this, {
                state ->
            run {
               // Log.d("btc-service", "SyncMode: ${BTCKitBuilder.syncMode}")
                when (state) {
                    is BitcoinCore.KitState.Synced -> {
                        Log.d("btc-service", "Synced")
                        progress = "Synced!"
                        manager?.notify(syncId, NotificationUtils.createBaseNotification("Synced!", this))
                        stopForeground(true)
                        //This will stop the receiver if it was triggered by the alarm
                        val receiverIntent = Intent(this, KitBroadcastReceiver::class.java)
                        this.sendBroadcast(receiverIntent)
                    }
                    is BitcoinCore.KitState.Syncing -> {
                        val progressStr = "Syncing the Blockchain: %${"%.2f".format(state.progress * 100)}\n"
                        var blockStr = ""
                        lastBlockInfo.observe(this, {
                            it?.let {

                                    blockInfo -> blockStr = SpannableStringBuilder("Block-Height: ${blockInfo.height}\nBlock-Date: ${dateFormat.format(Date(blockInfo.timestamp 
                                        * 1000))}").toString()
                            }
                            progress =progressStr+ blockStr


                        })
                      //  progress += lastBlockInfo.value
                        //Log.d("btc-service", "${syncState.value}")
                       // Log.d("btc-service", progress)
                       // Log.d("btc-service", "${syncState.hasActiveObservers()}")
                        manager?.notify(syncId, NotificationUtils.createSyncNotification(progressStr, blockStr,this))
                    }
                    is BitcoinCore.KitState.ApiSyncing -> {
                        Log.d("btc-service", "Api Syncing")
                        "api syncing ${state.transactions} txs"

                    }
                    is BitcoinCore.KitState.NotSynced -> {
                       // Log.d("btc-service", "Wrecked")
                        if(MainActivity.isActive) {
                            progress = "Unable to sync!"
                            manager?.notify(
                                syncId,
                                NotificationUtils.createSyncNotification(
                                    progress,
                                    "(Check your Internet connection)",
                                    this
                                )
                            )
                            "not synced ${state.exception.javaClass.simpleName}"
                        }
                    }
                }
            }
        })
        Log.d("btc-service", "Just b4 sync")
        bitcoinKit.start()
    } catch (e:Exception){
        Log.d("btc-service", "Service Exception: ${e.message}")
    }

    }
    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlockInfo.postValue(blockInfo)
    }
    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
       syncState.postValue(state)
    }

    override fun onBalanceUpdate(balance: BalanceInfo) {
        super.onBalanceUpdate(balance)
        val newBalanceStr = "New Balance: ${NumberFormatHelper.cryptoAmountFormat.format(balance.spendable / 100_000_000.0)} tBTC"
        manager?.notify(2,NotificationUtils.createBalanceNotification(newBalanceStr, this))
    }



}