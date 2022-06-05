package tbcode.example.cryptotestnetwallet

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
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
import kotlinx.coroutines.awaitAll
import tbcode.example.cryptotestnetwallet.utils.KitSyncService
import tbcode.example.cryptotestnetwallet.utils.kit_utils.BTCKitUtils
import java.security.SecureRandom

class MainActivity : AppCompatActivity() {

    companion object {
        var isActive = false
    }
    private lateinit var sharedPref: SharedPreferences
    init {
        //Disable dark mode here
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("btc-activity", "MainActivity onCreate is called!")
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_receive, R.id.nav_dash, R.id.nav_send))
        setupActionBarWithNavController(navController,appBarConfiguration)
        navView.setupWithNavController(navController)
        try {
            isActive = true
            sharedPref = this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)

            if(!sharedPref.contains(BTCKitUtils.getWalletID())){
                Log.d("btc-activity", "No wallet")
                generateWallet()
            }

            if(!isOnline()){ Log.d("btc-activity", "Not connected")
                throw Exception("No Connection Detected!")
            }
            val serviceIntent = Intent(this, KitSyncService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Log.d("btc-activity","Starting Foreground Service")
                startForegroundService(serviceIntent)
            } else{
                Log.d("btc-activity","Starting Regular Service")
                startService(serviceIntent)
            }
            Log.d("btc-activity","Service Component type: ${serviceIntent.component}")
            Log.d("btc-activity", "Is service running? : ${KitSyncService.isRunning}")


        }catch (e:Exception) {
            Toast.makeText(this,"Error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.d("btc-activity","Error: ${e.message}")
        }





    }


    private fun generateWallet(){
        val sb = StringBuilder()
        val entropy = ByteArray(Words.TWELVE.byteLength())
        SecureRandom().nextBytes(entropy)
        MnemonicGenerator(English.INSTANCE)
                .createMnemonic(entropy, sb::append)
        sharedPref.edit().putString(BTCKitUtils.getWalletID(), sb.toString()).apply()
        Toast.makeText(this.baseContext, "Generating Wallet", Toast.LENGTH_SHORT).show()
        seedDialog(sb.toString())

    }
    private fun seedDialog(seed:String){
        try {
            val alertDialog = AlertDialog.Builder(this)
            .setTitle("Seed Phrase Generated")
            .setMessage("Your seed phrase is: \n$seed\n" +
                    "Make sure to write it down or screenshot, and back it up somewhere!")
            .setPositiveButton("OK"){ _, _->
                Toast.makeText(this,
                    "You won't be able to send transactions until we're synced.(~2-5 min.)",
                    Toast.LENGTH_LONG).show()
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

    override fun onDestroy() {
        Log.d("btc-activity", "MainActivity onDestroy is called! $isFinishing")
        super.onDestroy()
        if (KitSyncService.isRunning)
        KitSyncService.stopSync()
        isActive = false

    }
}