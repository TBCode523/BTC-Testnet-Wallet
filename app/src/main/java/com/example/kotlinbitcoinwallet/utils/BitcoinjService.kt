package com.example.kotlinbitcoinwallet.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.bitcoinj.core.ECKey
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.KeyChainGroupStructure
import java.io.File

class BitcoinjService: Service() {
    private lateinit var walletAppKit: WalletAppKit
    private val netParams = TestNet3Params.get()
    override fun onBind(intent: Intent?): IBinder? =null

        }


