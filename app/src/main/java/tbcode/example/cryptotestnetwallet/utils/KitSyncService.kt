package tbcode.example.cryptotestnetwallet.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.litecoinkit.LitecoinKit
import tbcode.example.cryptotestnetwallet.MainActivity
import tbcode.example.cryptotestnetwallet.NumberFormatHelper
import java.text.SimpleDateFormat
import java.util.*

class KitSyncService(): LifecycleService(), BitcoinKit.Listener, LitecoinKit.Listener {
    //Role: This is where the syncing and notifications will be handled
    private  var manager:NotificationManager? = null
    private val syncId = 1
    private var progress = ""
    private lateinit var sharedPref: SharedPreferences
    init {
        Log.d("CT-service", "In innit")
        isRunning = true

    }

    companion object {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        //Every 15 min.
        //private val syncTiming = 900000L
        val syncState = MutableLiveData<BitcoinCore.KitState>()
        val lastBlockInfo = MutableLiveData<BlockInfo>()
        var coinKit: CoinKit? = null
        var isRunning = false
        private val _isKitAvailable = MutableLiveData<Boolean>().apply {
            value = false
        }
        val isKitAvailable:LiveData<Boolean> = _isKitAvailable
        fun stopSync(){
            Log.d("btc-db", "Stopping Foreground Service!")
            _isKitAvailable.value = false
           // instance?.stopForeground(STOP_FOREGROUND_REMOVE)
            isRunning = false
            coinKit?.kit?.stop()
            coinKit = null
           /* val alarmIntent = Intent(instance, KitBroadcastReceiver::class.java).let {
                    i -> PendingIntent.getBroadcast(instance, 0, i, PendingIntent.FLAG_IMMUTABLE)
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
                Date(it)
            }}!")
            */
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("btc-service", "Service onCreate")
        //if(coinKit == null) coinKit = CoinKit.tBTC(this, words!!)



    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        sharedPref = this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        val words = sharedPref.getString(CoinKit.walletId,null)?.split(" ")
        Log.d("btc-service", "Kit Builder words: $words")
        val coin = intent?.getIntExtra("coin", 0)
        when(coin){
            0 -> {
                Log.d("btc-service", "Picked BTCKit: $words")
                coinKit = CoinKit.tBTC(this, words!!)
            }
            1 ->{
                Log.d("btc-service", "Picked LTCkit: $words")
                coinKit = CoinKit.tLTC(this, words!!)
            }
        }
        coinKit.let {
            when (it) {
                is CoinKit.tBTC -> {
                    (it.kit as BitcoinKit).listener = this
                    syncState.value = it.kit.syncState
                    lastBlockInfo.value = it.kit.lastBlockInfo
                }
                is CoinKit.tLTC ->{
                    (it.kit as LitecoinKit).listener = this
                    syncState.value = it.kit.syncState
                    lastBlockInfo.value = it.kit.lastBlockInfo
                }
                else -> {}
            }

        }
        manager = NotificationUtils.createNotificationChannel(this)
        val syncNotification = NotificationUtils.createBaseNotification("Starting to Sync!", this)
        startForeground(syncId, syncNotification)
        syncKit()
        _isKitAvailable.value = true
        return START_STICKY
    }

    private fun syncKit() {
        //Sync the kit
        Log.d("btc-service", "Starting the Sync...")
        try {
            syncState.observe(this) { state ->
                run {
                    Log.d("btc-service", "SyncMode: ${coinKit?.kit?.syncState}")
                    when (state) {
                        is BitcoinCore.KitState.Synced -> {
                            Log.d("btc-service", "Synced")
                            progress = "Synced!"
                            manager?.notify(
                                syncId,
                                NotificationUtils.createBaseNotification("Synced!", this)
                            )
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            //This will stop the receiver if it was triggered by the alarm
                            //val receiverIntent = Intent(this, KitBroadcastReceiver::class.java)
                            //this.sendBroadcast(receiverIntent)
                        }
                        is BitcoinCore.KitState.Syncing -> {
                            val progressStr =
                                "Syncing the Blockchain: %${"%.2f".format(state.progress * 100)}\n"
                            var blockStr = ""
                            lastBlockInfo.observe(this) {
                                it?.let {
                                        blockInfo ->
                                    blockStr = SpannableStringBuilder(
                                        "Block-Height: ${blockInfo.height}\nBlock-Date: ${
                                            dateFormat.format(
                                                Date(
                                                    blockInfo.timestamp
                                                            * 1000
                                                )
                                            )
                                        }"
                                    ).toString()
                                }
                                progress = progressStr + blockStr
                            }
                            //  progress += lastBlockInfo.value
                            Log.d("btc-service", "${syncState.value}")
                            Log.d("btc-service", progress)
                            Log.d("btc-service", "${syncState.hasActiveObservers()}")
                            manager?.notify(
                                syncId,
                                NotificationUtils.createSyncNotification(coinKit!!.label,progressStr, blockStr, this)
                            )
                        }
                        is BitcoinCore.KitState.ApiSyncing -> {
                            Log.d("btc-service", "Api Syncing")
                            Log.d("btc-service", "api syncing ${state.transactions} txs")
                            "api syncing ${state.transactions} txs"
                        }
                        is BitcoinCore.KitState.NotSynced -> {
                            Log.d("btc-service", "Wrecked: ${state.exception.message} ")
                            if (MainActivity.isActive) {
                                progress = "Unable to sync!"
                                manager?.notify(
                                    syncId,
                                    NotificationUtils.createSyncNotification(coinKit!!.label,
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
            }
        Log.d("btc-service", "Just b4 sync")

        coinKit!!.kit.start()

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
        coinKit.let {
            when(it){
                is CoinKit.tBTC -> super<BitcoinKit.Listener>.onBalanceUpdate(balance)
                else -> {super<LitecoinKit.Listener>.onBalanceUpdate(balance)}
            }
        }

        val newBalanceStr = "New Balance: ${NumberFormatHelper.cryptoAmountFormat.format(balance.spendable / 100_000_000.0)} ${coinKit?.label}"
        manager?.notify(2,NotificationUtils.createBalanceNotification(coinKit!!.label, newBalanceStr, this))
    }
    override fun onDestroy() {
        Log.d("btc-service", "KitSync being destroyed")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSync()
        super.onDestroy()


    }
}