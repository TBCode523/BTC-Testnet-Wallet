package com.example.kotlinbitcoinwallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bitcoinkit.BitcoinKit
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private  var bitcoinKit: BitcoinKit):ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MainViewModel::class.java)){
            return ( MainViewModel(bitcoinKit) as T)
        }
        throw IllegalArgumentException("ViewModel only takes BTC-Kit")
    }
}