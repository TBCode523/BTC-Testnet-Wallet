package com.example.kotlinbitcoinwallet.utils

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.KeyChainGroupStructure
import java.io.File
import java.util.concurrent.TimeUnit

class BitcoinjWorker(context: Context, params: WorkerParameters,
                     val networkParameter: NetworkParameters,
                     val filesDir: File
): Worker(context,params) {
    override fun doWork(): Result {
        //TODO("Not yet implemented")
        val walletAppKit =  object: WalletAppKit(networkParameter,
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
        return Result.failure()
    }
    @Throws(Exception::class)
    fun btcSync(walletAppKit: WalletAppKit, seconds:Long){
        walletAppKit.startAsync()
        walletAppKit.awaitRunning(seconds,TimeUnit.SECONDS)
    }
}