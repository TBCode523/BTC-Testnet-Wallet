package tbcode.example.cryptotestnetwallet

import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import tbcode.example.cryptotestnetwallet.utils.CoinKit
import tbcode.example.cryptotestnetwallet.utils.KitSyncService
import java.security.SecureRandom

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerNav: NavigationView
    companion object {
        var isActive = false
        const val TAG = "btc-activity"
    }

    init {
        //Disable dark mode here
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate is called!")
        isActive = true
        sharedPref = this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        if(!sharedPref.contains(CoinKit.walletId)){
            Log.d(TAG, "No wallet")
            generateWallet()
        }
        val words = sharedPref.getString(CoinKit.walletId,null)?.split(" ")
        Log.d(TAG, "Kit Builder words: $words")
        var serviceIntent = Intent(this, KitSyncService::class.java)
        Log.d(TAG,"Starting Foreground Service")
        startForegroundService(serviceIntent)
        Log.d(TAG,"Service Component type: ${serviceIntent.component}")
        setContentView(R.layout.activity_main)
        drawerLayout = findViewById(R.id.drawer)
        drawerNav = findViewById(R.id.drawer_nav)
        toggle = ActionBarDrawerToggle(this,drawerLayout,R.string.open,R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawerNav.setNavigationItemSelectedListener {
            val kitClicked = resources.getResourceName(it.itemId).takeLast(4)
            Log.d(TAG, "kitclicked: $kitClicked coinkitlabel:${KitSyncService.coinKit?.label}")
            if(kitClicked != KitSyncService.coinKit?.label) {
                when (it.itemId) {
                    R.id.tBTC -> {
                        Toast.makeText(this, "Clicked on tBTC", Toast.LENGTH_SHORT).show()
                        stopService(serviceIntent)
                        serviceIntent = Intent(this, KitSyncService::class.java)
                        serviceIntent.putExtra("coin", 0)
                        startForegroundService(serviceIntent)
                    }
                    R.id.tLTC ->{
                        Toast.makeText(this, "Clicked on tLTC", Toast.LENGTH_SHORT).show()
                        stopService(serviceIntent)
                        serviceIntent = Intent(this, KitSyncService::class.java)
                        serviceIntent.putExtra("coin", 1)
                        startForegroundService(serviceIntent)
                    }
                }
            }
            else Toast.makeText(this, "Already on Kit ${it.title}", Toast.LENGTH_SHORT).show()
            true
        }
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_receive, R.id.nav_dash, R.id.nav_send), drawerLayout)
        setupActionBarWithNavController(navController,appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun generateWallet(){
        val sb = StringBuilder()
        val entropy = ByteArray(Words.TWELVE.byteLength())
        SecureRandom().nextBytes(entropy)
        MnemonicGenerator(English.INSTANCE)
                .createMnemonic(entropy, sb::append)
        sharedPref.edit().putString(CoinKit.walletId, sb.toString()).apply()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }


    override fun onDestroy() {
        Log.d("btc-activity", "MainActivity onDestroy is called! $isFinishing")
        super.onDestroy()
        if (KitSyncService.isRunning){
            val sIntent = Intent(this, KitSyncService::class.java)
            stopService(sIntent)
        }
        isActive = false
    }

}