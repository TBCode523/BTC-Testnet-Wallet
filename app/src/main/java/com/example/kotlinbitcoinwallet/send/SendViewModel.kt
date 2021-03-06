package com.example.kotlinbitcoinwallet.send
import FeePriority
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.bitcoinj.core.Coin
import java.net.URL

class SendViewModel : ViewModel() {

    // TODO: Implement the ViewModel

    val feeUrl = "https://mempool.space/api/v1/fees/recommended"
    lateinit var feePriority:FeePriority
    var txID = ""
    var sendAddress = ""





      fun generateFeePriority(feeUrl: String): FeePriority {
        val response = URL(feeUrl).readText()
        val gson = Gson()
        return gson.fromJson(response, FeePriority::class.java)
    }

    

}