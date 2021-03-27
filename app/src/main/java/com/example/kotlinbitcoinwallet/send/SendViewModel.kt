package com.example.kotlinbitcoinwallet.send
//import org.bitcoinj.core.Coin
import FeePriority
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.kotlinbitcoinwallet.NumberFormatHelper
import com.google.gson.Gson
import io.horizontalsystems.bitcoinkit.BitcoinKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.properties.Delegates

class SendViewModel : ViewModel() {

    private val feeUrl = "https://mempool.space/api/v1/fees/recommended"
    enum class FEE_RATE{LOW, MED, HIGH}
    val sats = 100000000
    lateinit var feePriority:FeePriority
    var sendAddress = ""
    var feeRate by Delegates.notNull<Int>()
    var amount:Long = 0
    var fee:Long = 0
    var formattedFee =NumberFormatHelper.cryptoAmountFormat.format( fee/ 100_000_000.0)
    init{
        CoroutineScope(IO).launch {

            feePriority = try {
                generateFeePriority(feeUrl)

            } catch (e:Exception){
                FeePriority(10,5,3)
            }
            feeRate = feePriority.medFee
            formattedFee = NumberFormatHelper.cryptoAmountFormat.format( feeRate/ 100_000_000.0)

        }
    }
      fun generateFeePriority(feeUrl: String): FeePriority {

        val response = URL(feeUrl).readText()
        val gson = Gson()
        return gson.fromJson(response, FeePriority::class.java)
    }
    fun generateFee(bitcoinKit: BitcoinKit): Long {
        try {


            fee = bitcoinKit.fee(amount, feeRate = feePriority.medFee)

            formattedFee = NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)
        } catch (e:Exception){
            fee = 0
            formattedFee = NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)
        }
        Log.d("SF-SVM","Generated fee:$formattedFee")
    return fee
    }
    fun generateFee(bitcoinKit: BitcoinKit,feeRate: FEE_RATE): Long {
        fee = try {
            when (feeRate) {
                FEE_RATE.MED -> bitcoinKit.fee(amount, feeRate = feePriority.medFee)
                FEE_RATE.LOW -> bitcoinKit.fee(amount, feeRate = feePriority.lowFee)
                else -> bitcoinKit.fee(amount, feeRate = feePriority.highFee)
            }
        } catch (e:Exception){
            0
        }
        formattedFee = "${NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)} BTC"
        Log.d("SF-SVM","Generated fee:$formattedFee")
        return fee
    }
    fun getFeeRate(feeRate: FEE_RATE):Int{
        return when(feeRate){
            FEE_RATE.LOW -> feePriority.lowFee
            FEE_RATE.HIGH -> feePriority.highFee
            else -> feePriority.medFee
        }
    }
    fun formatAmount():String{
        return NumberFormatHelper.cryptoAmountFormat.format(amount / 100_000_000.0)
    }
    fun formatFee():String{
        return  "${NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)} BTC"
    }
    fun formatTotal():String{
        return  "${NumberFormatHelper.cryptoAmountFormat.format((fee+amount) / 100_000_000.0)} BTC"
    }


    

}