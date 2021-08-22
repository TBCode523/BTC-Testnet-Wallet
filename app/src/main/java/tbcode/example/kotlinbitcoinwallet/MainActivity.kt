package tbcode.example.kotlinbitcoinwallet

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.kotlinbitcoinwallet.utils.KitBroadcastReceiver
import tbcode.example.kotlinbitcoinwallet.utils.KitSyncService
import tbcode.example.kotlinbitcoinwallet.utils.builders.BTCKitBuilder
import java.security.SecureRandom
import java.util.*

class MainActivity : AppCompatActivity() {


    private lateinit var bitcoinKit : BitcoinKit
    companion object {
        var isActive = false
       const val timeout = 120000L
    }

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_receive, R.id.nav_dash, R.id.nav_send))

        setupActionBarWithNavController(navController,appBarConfiguration)
        navView.setupWithNavController(navController)
        //Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        try {
                isActive = true
               sharedPref = this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)

            if(!sharedPref.contains(BTCKitBuilder.walletId)) btcDialog()
            val words = sharedPref.getString(BTCKitBuilder.walletId,null)?.split(" ")
            Log.d("btc-db","Seed Phrase: ${sharedPref.getString(BTCKitBuilder.walletId,"")}")
            Log.d("btc-db","syncMode: ${BTCKitBuilder.syncMode::class.java}")
            Log.d("btc-db","bip: ${BTCKitBuilder.bip}")
            //bitcoinKit = BitcoinKit(this,words!!, walletId, networkType, syncMode = syncMode, bip = bip)
            bitcoinKit = BitcoinKit(this,words!!, BTCKitBuilder.walletId,
                BTCKitBuilder.networkType, syncMode = BTCKitBuilder.syncMode, bip = BTCKitBuilder.bip)
            KitSyncService.bitcoinKit = bitcoinKit
            if(!isOnline()){ Log.d("btc-db", "Not connected")
                throw Exception("No Connection Detected!")
            }
            val serviceIntent = Intent(this, KitSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Log.d("btc-service","Starting Foreground Service")
                startForegroundService(serviceIntent)
            } else{
                Log.d("btc-service","Starting Regular Service")
                startService(serviceIntent)
            }
            Log.d("btc-db","Service Component type: ${serviceIntent.component}")
       //
            Log.d("btc-service", "Is service running? : ${KitSyncService.isRunning}")


        }catch (e:Exception) {
            Toast.makeText(this,"Error: ${e.message}", Toast.LENGTH_LONG).show()
        }




    }

    private fun clearDialogue() {
        val alertDialog = AlertDialog.Builder(this)
                .setTitle("Clear Wallet?")
                .setMessage(" Want to re-sync?(This could take a while")
                .setPositiveButton("OK"){ _, _->
                BitcoinKit.clear(this, BTCKitBuilder.networkType, BTCKitBuilder.walletId)
                    bitcoinKit.refresh()

                }.create()
        alertDialog.show()
    }
    private fun btcDialog(){
        val sb = StringBuilder()
        val entropy = ByteArray(Words.TWELVE.byteLength())
        SecureRandom().nextBytes(entropy)
        MnemonicGenerator(English.INSTANCE)
                .createMnemonic(entropy, sb::append)
        sharedPref.edit().putString(BTCKitBuilder.walletId, sb.toString()).apply()
        BTCKitBuilder.syncMode = BitcoinCore.SyncMode.NewWallet()
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("No Wallet found")
            .setMessage("You don't have a wallet yet we'll create one for you!")
            .setPositiveButton("OK"){ _, _->



                seedDialog(sb.toString())
            }.create()
        alertDialog.show()

    }
    private fun seedDialog(seed:String){
        try {


        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Seed Phrase Generated")
            .setMessage("Your seed phrase is: \n $seed \n" +
                    "Make sure to write it down or back it up somewhere!")
            .setPositiveButton("OK I wrote it down"){ _, _->
                Toast.makeText(this,"You won't be able to send transactions until we're synced.(~2-5 min.)", Toast.LENGTH_SHORT).show()




            }.create()
        alertDialog.show()
        } catch (e:Exception){
            Toast.makeText(this,"Seed retrieval failed!", Toast.LENGTH_SHORT).show()
        }

    }

    private fun isOnline(): Boolean {
        val connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    override fun onPause() {
        super.onPause()
        isActive = false
        val alarmIntent = Intent(this, KitBroadcastReceiver::class.java).let {
                i -> PendingIntent.getBroadcast(this, 0, i, 0)
        }

        val alarmMgr =
            this.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmMgr?.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + timeout,
            alarmIntent
        )
        val date = Calendar.getInstance().time
        val newDate = Date(date.time + timeout)
        Log.d("btc-alert", "Setting TimeOut to ${newDate}!")

    }
    override fun onDestroy() {
        Log.d("btc-alert", "onDestroy is called!")
        super.onDestroy()

    }
}