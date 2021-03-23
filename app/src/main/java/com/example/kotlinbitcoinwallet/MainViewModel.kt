package com.example.kotlinbitcoinwallet

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit

class MainViewModel(val bitcoinKit: BitcoinKit): ViewModel(), BitcoinKit.Listener {
//TODO Notify changes to kit balance and use variables to tell when chain is syncing
    //Role of MainViewModel: Keep track of updates and events
    val state = MutableLiveData<BitcoinCore.KitState>()
    private var started = false
    var ending = "BTC"


    init{
        start()
        bitcoinKit.listener = this
      //  bitcoinKit.syncState

     //   state.observe()
        if(bitcoinKit.networkName.startsWith("test")) ending = "tBTC"
        Log.d("btc-db","Current address: ${bitcoinKit.receiveAddress()}")
    }
    private fun start() {
        if (started) return
        else{
            started =true
            bitcoinKit.start()

        }
    }
    fun stop() {
        bitcoinKit.stop()
    }
    override fun onTransactionsUpdate(
        inserted: List<TransactionInfo>,
        updated: List<TransactionInfo>
    ) {
        //TODO Create Notification
        super.onTransactionsUpdate(inserted, updated)
    }

    override fun onBalanceUpdate(balance: BalanceInfo) {
        //TODO create notification
        super.onBalanceUpdate(balance)
    }

}