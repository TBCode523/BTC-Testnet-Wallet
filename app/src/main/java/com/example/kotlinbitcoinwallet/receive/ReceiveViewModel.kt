package com.example.kotlinbitcoinwallet.receive
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoinkit.BitcoinKit

class ReceiveViewModel : ViewModel() {
//Role: Generate Addresses and QRCodes
    var text:String = "Receive Fragment"
 //   var currentAddress: Address? = null
    var currentAddressString:String = "25"

    fun generateAddress(bitcoinKit: BitcoinKit):String{
        currentAddressString = bitcoinKit.receiveAddress()
        return  currentAddressString
    }
    /*

    fun generateNewAddress(wallet: Wallet):String{
        currentAddress = wallet.freshReceiveAddress()
        currentAddressString = (currentAddress as SegwitAddress).toBech32()
        return  currentAddressString
    }

    fun generateCurrentAddress(wallet: Wallet):String{
        currentAddress = wallet.currentReceiveAddress()
        currentAddressString = (currentAddress as SegwitAddress).toBech32()
        return  currentAddressString
    }*/
    //Returns a random string for testing
    private fun generateRandomString(length: Int):String{
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'1')

        return (1..length).map { charset.random()}.joinToString("")
    }

}