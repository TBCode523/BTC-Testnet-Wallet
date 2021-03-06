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
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.base.Joiner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroupStructure
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val testNetKey ="testnet_phrase"
    private var scriptType = Script.ScriptType.P2WPKH
    private var networkParameter = TestNet3Params.get()

      lateinit var walletAppKit:WalletAppKit



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
            walletAppKit =   object: WalletAppKit(networkParameter,
                    Script.ScriptType.P2WPKH, KeyChainGroupStructure.DEFAULT,filesDir,"btc-kit"){
                override fun onSetupCompleted() {
                    if (wallet().keyChainGroupSize < 1)
                        wallet().importKey( ECKey())
                    val chainFile = File(filesDir,"btc-kit.spvchain")
                    val walletFile = File(filesDir,"btc-kit.wallet")
                    val str = "Chain File exist?: ${chainFile.exists()} \n Wallet File exist?:${walletFile.exists()}\n " +
                            "Chain FIle Path: ${chainFile.absolutePath}\nWallet Path ${walletFile.absolutePath} "
                Log.w("BD", str)


                }

            }
            Toast.makeText(this, "Syncing chain", Toast.LENGTH_LONG).show()

            walletAppKit.setBlockingStartup(false).startAsync().awaitRunning(60,TimeUnit.SECONDS)
            balanceDialog()
            walletAppKit.wallet().addCoinsReceivedEventListener { wallet1, tx, prevBalance, newBalance ->
                System.out.println("-----> coins resceived: " + tx.txId)
                System.out.println("received: " + tx.getValue(wallet1))
            }

            Log.v("WB","Wallet Balance:${walletAppKit.wallet().balance.toPlainString()} BTC")
        }catch (e:Exception) {
            Toast.makeText(this,"Oh Naaah: ${e.message}", Toast.LENGTH_LONG).show()
        }




    }

    override fun onStart() {

        super.onStart()



    }


    override fun onDestroy() {

        walletAppKit.stopAsync()
        walletAppKit.awaitTerminated()
        super.onDestroy()
    }

    private fun btcDialog(){
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("No Wallet found")
            .setMessage("You don't have a wallet yet we'll create one for you!")
            .setPositiveButton("OK"){ _, _->


               val wallet = walletAppKit.wallet()
                val mnemonicCode =   wallet?.keyChainSeed?.mnemonicCode
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
        var str = "Creation-Time:${walletAppKit.wallet().earliestKeyCreationTime} \n" + "AppKit Running: ${walletAppKit.isRunning}\n"+
                " File Directory:${walletAppKit.directory().canonicalPath}\nTransactions:\n"
        try {
    for(x in walletAppKit.wallet().transactionsByTime){
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
    private fun generateWalletAppKit():WalletAppKit{
        return  object: WalletAppKit(networkParameter,
                Script.ScriptType.P2WPKH, KeyChainGroupStructure.DEFAULT,filesDir,"btc-kit"){
            override fun onSetupCompleted() {
                if (wallet().keyChainGroupSize < 1)
                    wallet().importKey( ECKey())

                walletAppKit.wallet().addCoinsReceivedEventListener { wallet1, tx, prevBalance, newBalance ->
                    System.out.println("-----> coins resceived: " + tx.txId)
                    System.out.println("received: " + tx.getValue(wallet1))
                }



            }
        }
    }

    fun getScriptType() = scriptType
    fun getNetworkParameters() = networkParameter
    fun getWallet(): Wallet = walletAppKit.wallet()
    fun getPeerGroup():PeerGroup = walletAppKit.peerGroup()

}