package tbcode.example.kotlinbitcoinwallet.send
import FeePriority
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import tbcode.example.kotlinbitcoinwallet.NumberFormatHelper
import java.net.URL

class SendViewModel : ViewModel() {

    enum class FEE_RATE{LOW, MED, HIGH}
    val sats = 100000000
    private lateinit var feePriority:FeePriority
    var sendAddress = ""
    var amount:Long = 0
    var fee:Long = 0
    var formattedFee: String = NumberFormatHelper.cryptoAmountFormat.format( fee/ 100_000_000.0)
    var timeLockInterval: LockTimeInterval? = null
    var errorMsg = ""

    init{
        CoroutineScope(IO).launch {

            feePriority = try {
                generateFeePriority()

            } catch (e:Exception){
                Log.d("SF-SVM", "generateFeePriority Error: $e")
                FeePriority(10,5,3)
            }

            formattedFee = NumberFormatHelper.cryptoAmountFormat.format( feePriority.medFee/ 100_000_000.0)

        }
    }
      private fun generateFeePriority(feeUrl: String = "https://mempool.space/api/v1/fees/recommended"): FeePriority {

        val response = URL(feeUrl).readText()
          Log.d("SF-SVM","URL Response: $response")
        val gson = Gson()
        return gson.fromJson(response, FeePriority::class.java)
    }

    fun generateFee(bitcoinKit: BitcoinKit,feeRate: FEE_RATE): Boolean {
        errorMsg = ""
        fee = try {
            Log.d("SF-SVM", "amount: $amount")
            when (feeRate) {
                FEE_RATE.MED -> bitcoinKit.fee(amount, feeRate = feePriority.medFee)
                FEE_RATE.LOW -> bitcoinKit.fee(amount, feeRate = feePriority.lowFee)
                else -> bitcoinKit.fee(amount, feeRate = feePriority.highFee)
            }
        } catch (e:SendValueErrors.InsufficientUnspentOutputs){
            Log.d("SF-SVM", "generateFee Error: $e")
            errorMsg = "Insufficient Balance"
            0
        }
        catch (e: SendValueErrors.Dust){
            Log.d("SF-SVM", "generateFee Error: $e")
            errorMsg = "You must send at least 0.00001 tBTC"
            0
        }
        catch (e: Error){
            Log.d("SF-SVM", "generateFee Error: $e")
            errorMsg = "Fee Generator Failed"
            0
        }
        if(errorMsg.isNotBlank()){
            Log.d("SF-SVM","Generation Failed")
            return false
        }
        formattedFee = "${NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)} tBTC"
        Log.d("SF-SVM","Generated fee:$formattedFee")
        return true
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
        return  "${NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)} tBTC"
    }
    fun formatTotal():String{
        return  "${NumberFormatHelper.cryptoAmountFormat.format((fee+amount) / 100_000_000.0)} tBTC"
    }
    fun getPluginData(): MutableMap<Byte, IPluginData> {
        val pluginData = mutableMapOf<Byte, IPluginData>()
        timeLockInterval?.let {
            pluginData[HodlerPlugin.id] = HodlerData(it)
        }
        return pluginData
    }


    

}