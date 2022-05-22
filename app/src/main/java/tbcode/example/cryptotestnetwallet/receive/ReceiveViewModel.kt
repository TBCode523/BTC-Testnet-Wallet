package tbcode.example.cryptotestnetwallet.receive
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoinkit.BitcoinKit

class ReceiveViewModel : ViewModel() {
//Role: Generate Addresses and QRCodes
    var currentAddressString:String = ""

    fun generateAddress(bitcoinKit: BitcoinKit):String{
        currentAddressString = bitcoinKit.receiveAddress()
        return  currentAddressString
    }

    //Returns a random string for testing
    private fun generateRandomString(length: Int):String{
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'1')

        return (1..length).map { charset.random()}.joinToString("")
    }

}