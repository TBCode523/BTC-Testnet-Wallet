package com.example.kotlinbitcoinwallet.receive

import androidx.lifecycle.ViewModel
import org.bitcoinj.core.Address
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.KeyChain
import org.bitcoinj.wallet.Wallet

class ReceiveViewModel : ViewModel() {

    var text:String = "Receive Fragment"
    var currentAddress: Address? = null
    var currentAddressString:String = "25"



    fun generateNewAddress(wallet: Wallet):String{
        currentAddress = wallet.freshReceiveAddress()
        currentAddressString = (currentAddress as SegwitAddress).toBech32()
        return  currentAddressString
    }
    fun generateCurrentAddress(wallet: Wallet):String{
        currentAddress = wallet.currentReceiveAddress()
        currentAddressString = (currentAddress as SegwitAddress).toBech32()
        return  currentAddressString
    }
    //Returns a random string for testing
    private fun generateRandomString(length: Int):String{
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'1')

        return (1..length).map { charset.random()}.joinToString("")
    }

}