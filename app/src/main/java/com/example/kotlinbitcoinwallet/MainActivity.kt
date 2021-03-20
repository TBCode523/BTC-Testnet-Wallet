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
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoinkit.BitcoinKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

import java.io.File
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), BitcoinKit.Listener {

    private val testNetKey ="testnet_phrase"
    private lateinit var bitcoinKit : BitcoinKit
    companion object {

        val words = "used ugly meat glad balance divorce inner artwork hire invest already piano".split(" ")
        private val walletId = "MyWallet"
        private var networkType = BitcoinKit.NetworkType.TestNet
        private var syncMode = BitcoinCore.SyncMode.Api()
        private var bip = Bip.BIP44


        fun setNetworkType(type: BitcoinKit.NetworkType){
            networkType = type
        }

    }
    lateinit var viewModel: MainViewModel
//TODO use shared preferences to determine if a wallet has been created
    //private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_receive, R.id.nav_dash,R.id.nav_send))

        setupActionBarWithNavController(navController,appBarConfiguration)
        navView.setupWithNavController(navController)

        try {

            bitcoinKit = BitcoinKit(this,words,walletId,networkType, syncMode = syncMode, bip = bip)
            viewModel = ViewModelProvider(this, MainViewModelFactory(bitcoinKit)).get(MainViewModel::class.java)
            Toast.makeText(this,"Syncing",Toast.LENGTH_SHORT).show()
            viewModel.state.observe(this, androidx.lifecycle.Observer { state ->
                when(state){
                    is BitcoinCore.KitState.Synced -> {
                        Log.d("btc-kit-sync", "SYNCED!")
                        Toast.makeText(this,"Synced",Toast.LENGTH_SHORT).show()
                    }
                    is BitcoinCore.KitState.Syncing ->{
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




    override fun onDestroy() {


        super.onDestroy()
    }

    private fun btcDialog(){
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("No Wallet found")
            .setMessage("You don't have a wallet yet we'll create one for you!")
            .setPositiveButton("OK"){ _, _->




       //     sharedPreferences.edit().putString(testNetKey, Joiner.on(" ").join(mnemonicCode)).apply()


                seedDialog()
            }.create()
        alertDialog.show()

    }
    private fun seedDialog(){
        try {


        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Seed Phrase Generated")
            .setMessage("Your seed phrase is: \n " +
                    "Make sure to write it down or back it up!")
            .setPositiveButton("OK I wrote it down"){ _, _->





            }.create()
        alertDialog.show()
        } catch (e:Exception){
            Toast.makeText(this,"Seed retrieval failed!", Toast.LENGTH_SHORT).show()
        }
    }
    private fun balanceDialog(){
        /*
        var str = "Creation-Time:${} \n" + "AppKit Running: ${}\n"+
                " File Directory:${}\nTransactions:\n"
        try {
    for(x in bitcoinKit.tr){
        str+="${x.txId}\n"
    }
            str+="Peer # and running:${walletAppKit.peerGroup().numConnectedPeers()} ${walletAppKit.peerGroup().isRunning}\n"
            str+="Chain Height: ${walletAppKit.chain().chainHead.height}\n"
            str+="Balance: ${walletAppKit.wallet().balance.toPlainString() } BTC Received: ${walletAppKit.wallet().totalReceived.toPlainString() } BTC Sent: ${walletAppKit.wallet().totalSent.toPlainString() } BTC\n"
            str+="Directory Status: ${walletAppKit.directory().totalSpace} bytes\n Can Read File?:${walletAppKit.directory().canRead()} Can Write File?:${walletAppKit.directory().canWrite()}"
            Log.w("BD",str)
            val alertDialog = AlertDialog.Builder(this)
                    .setTitle("Summary")
                    .setMessage(str)
                    .setPositiveButton("OK"){ _, _->





                    }.create()
            alertDialog.show()
        } catch (e:Exception){
            Toast.makeText(this,"Seed retrieval failed!", Toast.LENGTH_SHORT).show()
            e.message?.let { errorDialog(it) }
        }

         */
    }
    private fun errorDialog(errorMsg:String){
        try {


            val alertDialog = AlertDialog.Builder(this)
                    .setTitle("ERROR")
                    .setMessage("EXCEPTION WAS THROWN:${errorMsg} ")
                    .setPositiveButton("OK"){ _, _->





                    }.create()
            alertDialog.show()
        } catch (e:Exception){
            Toast.makeText(this,"Seed retrieval failed!", Toast.LENGTH_SHORT).show()
        }
    }

 //   fun getViewModelBTCKit(): BitcoinKit = viewModel.bitcoinKit
    fun getBTCKit(): BitcoinKit = bitcoinKit


}