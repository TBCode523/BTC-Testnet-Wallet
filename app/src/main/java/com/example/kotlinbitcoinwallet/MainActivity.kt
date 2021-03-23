package com.example.kotlinbitcoinwallet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.kotlinbitcoinwallet.send.SendViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.base.Joiner
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.core.Wallet
import io.horizontalsystems.bitcoinkit.BitcoinKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

import java.io.File
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), BitcoinKit.Listener {


    private lateinit var bitcoinKit : BitcoinKit
    companion object {

        private val walletId = "MyWallet"
        private var networkType = BitcoinKit.NetworkType.TestNet
        private var syncMode = BitcoinCore.SyncMode.Api()
        private var bip = Bip.BIP84


        fun setNetworkType(type: BitcoinKit.NetworkType){
            networkType = type
        }

    }
    lateinit var viewModel: MainViewModel
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_receive, R.id.nav_dash,R.id.nav_send))

        setupActionBarWithNavController(navController,appBarConfiguration)
        navView.setupWithNavController(navController)

        try {
               sharedPref = this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)

            if(!sharedPref.contains(walletId)) btcDialog()
            val words = sharedPref.getString(walletId,null)?.split(" ")
            Log.d("btc-db","Seed Phrase: ${sharedPref.getString(walletId,"")}")
            bitcoinKit = BitcoinKit(this,words!!,walletId,networkType, syncMode = syncMode, bip = bip)
            viewModel = ViewModelProvider(this, MainViewModelFactory( bitcoinKit)).get(MainViewModel::class.java)
            viewModel.state.observe(this, { state ->
                when(state){
                    is BitcoinCore.KitState.Synced -> {
                        Log.d("btc-kit-sync", "SYNCED!")
                        Toast.makeText(this,"Synced",Toast.LENGTH_SHORT).show()
                    }
                    is BitcoinCore.KitState.Syncing ->{
                        Toast.makeText(this,"Syncing",Toast.LENGTH_SHORT).show()
                        Log.d("btc-kit-syncing", "syncing ${"%.3f".format(state.progress)}")
                    }
                    is BitcoinCore.KitState.ApiSyncing -> {
                        Log.d("btc-kit-api","api syncing ${state.transactions} txs")
                    }
                    is BitcoinCore.KitState.NotSynced -> {
                        Log.d("btc-kit-notsync","not synced ${state.exception.javaClass.simpleName}")
                    }
                }
            })

        }catch (e:Exception) {
            Toast.makeText(this,"Error: ${e.message}", Toast.LENGTH_LONG).show()
        }




    }

    override fun onStop() {
        super.onStop()
        viewModel.stop()
    }
    private fun btcDialog(){
        val sb = StringBuilder()
        val entropy = ByteArray(Words.TWELVE.byteLength())
        SecureRandom().nextBytes(entropy)
        MnemonicGenerator(English.INSTANCE)
                .createMnemonic(entropy, sb::append)
        sharedPref.edit().putString(walletId, sb.toString()).apply()
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



}