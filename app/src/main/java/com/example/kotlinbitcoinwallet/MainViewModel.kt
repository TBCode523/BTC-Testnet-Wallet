package com.example.kotlinbitcoinwallet

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit

class MainViewModel(val bitcoinKit: BitcoinKit): ViewModel(), BitcoinKit.Listener {
//TODO Notify changes to kit balance and use variables to tell when chain is syncing
    //Role of MainViewModel: Keep track of updates and events
    val state = MutableLiveData<BitcoinCore.KitState>()
    val lastBlock = MutableLiveData<BlockInfo>()
    private var started = false
    var ending = "BTC"


    init{
        start()
        bitcoinKit.listener = this
      //  bitcoinKit.syncState

     //   state.observe()
        state.value = bitcoinKit.syncState
        lastBlock.value = bitcoinKit.lastBlockInfo
        if(bitcoinKit.networkName.startsWith("test")) ending = "tBTC"

        Log.d("btc-db","Current address: ${bitcoinKit.receiveAddress()}")
    }
    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        this.lastBlock.postValue(blockInfo)
    }
    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        this.state.postValue(state)
    }
    private fun start() {
        if (started) return
        else{
            started =true
            bitcoinKit.start()

        }
    }
    fun stop() {

        started =false
        bitcoinKit.stop()

    }

}