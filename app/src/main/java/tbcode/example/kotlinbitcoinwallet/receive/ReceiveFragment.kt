package tbcode.example.kotlinbitcoinwallet.receive
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import io.horizontalsystems.bitcoinkit.BitcoinKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tbcode.example.kotlinbitcoinwallet.R
import tbcode.example.kotlinbitcoinwallet.utils.KitSyncService

class ReceiveFragment : Fragment() {



    private lateinit var viewModel: ReceiveViewModel
    private lateinit var receiveTxt: TextView
    private lateinit var qrCode: ImageView
    private lateinit var generateBtn:Button
    private lateinit var bitcoinKit: BitcoinKit
    private lateinit var sharedPref: SharedPreferences
    private  lateinit var  checkBox: CheckBox
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.receive_fragment, container, false)
         receiveTxt = root.findViewById(R.id.tv_Receive)

         qrCode= root.findViewById(R.id.qr_code)
         generateBtn = root.findViewById(R.id.generate_btn)
        sharedPref = this.requireContext().getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        if(!sharedPref.contains("warning"))  sharedPref.edit().putBoolean("warning", true).apply()


        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        try {
            bitcoinKit =  KitSyncService.bitcoinKit
            viewModel = ViewModelProvider(this).get(ReceiveViewModel::class.java)
            receiveClick()
            generateBtn.setOnClickListener {
                receiveClick()
            }
        } catch (e:Exception){
            Toast.makeText(context,"Wallet is Null",Toast.LENGTH_LONG).show()
        }

    }
    private fun receiveClick() {

        try {

            val generatedAddress: String = viewModel.generateAddress(bitcoinKit)
            qrCode.setImageBitmap(null)
            Toast.makeText(this.context, "Generating QRCode", Toast.LENGTH_SHORT).show()
          if(sharedPref.getBoolean("warning", true)) warningDialogue()


            CoroutineScope(IO).launch {

                generateQRCode(generatedAddress)
            }

            receiveTxt.text = generatedAddress



            Toast.makeText(
                this.context,
                "QRCode Successfully Generated: ${viewModel.currentAddressString}",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this.context, "Failed to Generate Address: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private suspend fun setQRCode(bitmap: Bitmap){

        withContext(Main){

            qrCode.setImageBitmap(bitmap)
        }
    }
    private suspend fun generateQRCode(text:String){
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
                Log.d(TAG, "generateQRCode: ${e.message}")

            }
            setQRCode(bitmap)

        }
    /*
    private fun showAllAddresses(bitcoinKit: BitcoinKit){

         val addresses = bitcoinKit.receivePublicKey().used()
         var str = "Balance: ${wallet.balance}"
         for (address in addresses){
             str+="\n"+ (address as SegwitAddress).toBech32()
         }
         for(word in wallet.keyChainSeed.mnemonicCode!!) {
             str += "\n" + word
         }

         val alertDialog = AlertDialog.Builder(this.requireContext())
             .setTitle("Used Addresses")
             .setMessage(str)
             .setPositiveButton("OK"){ _, _->






             }.create()
         alertDialog.show()
     }*/
    private fun warningDialogue(){
        val view= View.inflate(this.requireContext(), R.layout.dialog_checkbox, null)
         checkBox = view.findViewById(R.id.dialog_checkBox)
        AlertDialog.Builder(this.requireContext())
            .setTitle("WARNING")
            .setView(view)
            .setMessage("This is a Testnet Wallet! \n" +
                    "Do Not Send Mainet Bitcoin (BTC) to This Address!\nWe Are Not Responsible For Lost Funds!")
            .setPositiveButton("OK"){_ , _ ->
                if(checkBox.isChecked) sharedPref.edit().putBoolean("warning", false).apply()
            }
            .show()
    }

}