package tbcode.example.cryptotestnetwallet.receive
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import io.horizontalsystems.bitcoinkit.BitcoinKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tbcode.example.cryptotestnetwallet.utils.CoinKit

class ReceiveViewModel : ViewModel() {
    //Generates Addresses & QRCodes for ReceiveFragment
    private val _currentAddress = MutableLiveData<String>().apply {
        value = ""
    }
    val currentAddress:LiveData<String>
            get() = _currentAddress

    private val _currentQRCode = MutableLiveData<Bitmap>()
    val currentQRCode:LiveData<Bitmap>
        get() = _currentQRCode

    fun generateAddress(bitcoinKit: BitcoinKit):String{
        _currentAddress.value = bitcoinKit.receiveAddress()
        return _currentAddress.value!!
    }
    fun generateAddress(coinKit: CoinKit):String{
        _currentAddress.value = coinKit.kit.receiveAddress()
        return _currentAddress.value!!
    }
    fun generateQRCode(text:String){
        viewModelScope.launch {
          val qrCode =  createBitmap(text)
          _currentQRCode.value = qrCode
        }
    }
    private suspend fun createBitmap(text:String):Bitmap = withContext(Dispatchers.Main){
        val width = 750
        val height = 750
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val codeWriter = MultiFormatWriter()
        try {
            val bitMatrix = codeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        } catch (e: WriterException) {
            Log.d(ContentValues.TAG, "generateQRCode: ${e.message}")

        }

        return@withContext bitmap

    }
    //Returns a random string for testing
    fun clearFields() {
        _currentAddress.value = null
    }
    private fun generateRandomString(length: Int):String{
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'1')

        return (1..length).map { charset.random()}.joinToString("")
    }



}