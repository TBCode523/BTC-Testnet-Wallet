package tbcode.example.cryptotestnetwallet.send
import FeePriority
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tbcode.example.cryptotestnetwallet.NumberFormatHelper
import java.net.URL

class SendViewModel : ViewModel() {
    enum class FEE_RATE{LOW, MED, HIGH}

    private val _feeP = MutableLiveData<FeePriority>()
    val feeP:LiveData<FeePriority>
        get() = _feeP

    val _feeR = MutableLiveData<FEE_RATE>()
    val feeR:LiveData<FEE_RATE>
        get() = _feeR

    var fee:Long = 0
    var timeLockInterval: LockTimeInterval? = null
    var errorMsg = ""
    companion object{
        const val TAG = "CT-SF-SVM"
        const val sats = 100000000
    }

    init{
        viewModelScope.launch {
            withContext(IO){
                val feePriority = try {
                    generateFeePriority()
                } catch (e:Exception){
                    Log.d(TAG, "generateFeePriority Error: $e")
                    FeePriority(1,1,1)
                }
                Log.d(TAG, "feePriority: $feePriority")
                _feeP.postValue(feePriority)
                Log.d(TAG, "feeP: ${_feeP.value}")
            }
        }
    }

    private fun generateFeePriority(feeUrl: String = "https://mempool.space/api/v1/fees/recommended"): FeePriority {
        val response = URL(feeUrl).readText()
        Log.d(TAG, "URL Response: $response")
        val gson = Gson()
        return gson.fromJson(response, FeePriority::class.java)
    }

    fun generateFee(bitcoinKit: BitcoinKit,feeRate: FEE_RATE, amount:Long): Boolean {
        errorMsg = ""
        fee = try {
            Log.d(TAG, "amount: $amount")
            when (feeRate) {
                FEE_RATE.MED -> bitcoinKit.fee(amount, feeRate = feeP.value!!.medFee)
                FEE_RATE.LOW -> bitcoinKit.fee(amount, feeRate = feeP.value!!.lowFee)
                else -> bitcoinKit.fee(amount, feeRate = feeP.value!!.highFee)
            }
        } catch (e:SendValueErrors.InsufficientUnspentOutputs){
            Log.d(TAG, "generateFee Error: $e")
            errorMsg = "Insufficient Balance"
            0
        }
        catch (e:SendValueErrors.EmptyOutputs){
            Log.d(TAG, "generateFee Error: $e")
            errorMsg = "Insufficient Balance(Empty Outputs)"
            0
        }
        catch (e: SendValueErrors.Dust){
            Log.d(TAG, "generateFee Error: $e")
            errorMsg = "You must send at least 0.00001 tBTC"
            0
        }
        catch (e: SendValueErrors){
            Log.d(TAG, "generateFee Error: $e")
            errorMsg = "Fee Generator Failed"
            0
        }
        if(errorMsg.isNotBlank()){
            Log.d(TAG,"Generation Failed")
            return false
        }
        _feeR.value = feeRate
        Log.d(TAG,"New feeRate:${_feeR.value}")
        Log.d(TAG,"Generated fee:${formatFee()}")
        return true
    }

    fun getFeeRate(feeRate: FEE_RATE):Int{
        return when(feeRate){
            FEE_RATE.LOW -> feeP.value!!.lowFee
            FEE_RATE.HIGH -> feeP.value!!.highFee
            else -> feeP.value!!.medFee
        }
    }

    fun formatAmount(amount: Long):String{
        return NumberFormatHelper.cryptoAmountFormat.format(amount / 100_000_000.0)
    }
    fun formatFee():String{
        return  "${NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)} tBTC"
    }
    fun formatTotal(amount: Long):String{
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