package tbcode.example.cryptotestnetwallet.receive
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
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
import tbcode.example.cryptotestnetwallet.R
import tbcode.example.cryptotestnetwallet.utils.KitSyncService



class ReceiveFragment : Fragment() {



    private lateinit var viewModel: ReceiveViewModel
    private lateinit var receiveTxt: TextView
    private lateinit var qrCode: ImageView
    private lateinit var generateBtn:Button
    private lateinit var faucetBtn:Button
    private lateinit var cryptoKit: BitcoinKit
    private lateinit var sharedPref: SharedPreferences
    private  lateinit var  checkBox: CheckBox
    private lateinit var amountTxt:EditText
    companion object{
        const val TAG = "RF"
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.receive_fragment, container, false)
        receiveTxt = root.findViewById(R.id.tv_Receive)
        amountTxt = root.findViewById(R.id.ev_amount_rf)
        qrCode= root.findViewById(R.id.qr_code)
        generateBtn = root.findViewById(R.id.generate_btn)
        faucetBtn = root.findViewById(R.id.faucet_btn)
        sharedPref = this.requireContext().getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        if(!sharedPref.contains("warning"))  sharedPref.edit().putBoolean("warning", true).apply()


        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        try {
            cryptoKit =  KitSyncService.bitcoinKit
            viewModel = ViewModelProvider(this).get(ReceiveViewModel::class.java)
           if (viewModel.currentAddress.value.isNullOrEmpty() || viewModel.currentAddress.value!!.isBlank()) {
               receiveClick()
           }
            else{
                receiveTxt.text = viewModel.currentAddress.value
                qrCode.setImageBitmap(viewModel.currentQRCode.value)

           }
            generateBtn.setOnClickListener {
                Log.d(TAG,"QR-Amount: " + amountTxt.text)
                receiveClick(amount = amountTxt.text.toString())
            }
            faucetBtn.setOnClickListener {
                val uriStr = "https://testnet-faucet.mempool.co/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                ContextCompat.startActivity(requireContext(), intent, null)
            }
            amountTxt.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_DONE) {
                    receiveClick(amount = amountTxt.text.toString())
                    true
                }
                else false
            }

        } catch (e:Exception){
            Toast.makeText(context,"Wallet is Null",Toast.LENGTH_LONG).show()
        }

    }
    private fun receiveClick(amount: String = "") {

        try {

            val generatedAddress: String = viewModel.generateAddress(cryptoKit)
            qrCode.setImageBitmap(null)
            Toast.makeText(this.context, "Generating QRCode", Toast.LENGTH_SHORT).show()
            if(sharedPref.getBoolean("warning", true)) warningDialogue()


           /* CoroutineScope(IO).launch {
                if (amount.isBlank())
                generateQRCode("bitcoin:$generatedAddress")

                else
                    generateQRCode("bitcoin:$generatedAddress?amount=$amount")

            }*/
            if (amount.isBlank()) viewModel.generateQRCode("bitcoin:$generatedAddress")
            else viewModel.generateQRCode("bitcoin:$generatedAddress?amount=$amount")

            Log.d(TAG, "generatedAddress: $generatedAddress")
            Log.d(TAG, "currentAddress: ${viewModel.currentAddress.value}")
            Log.d(TAG, "currentQRCode: ${viewModel.currentQRCode.value}")
            receiveTxt.text = viewModel.currentAddress.value
            qrCode.setImageBitmap(viewModel.currentQRCode.value)


            Toast.makeText(
                this.context,
                "QRCode Successfully Generated: ${viewModel.currentAddress.value}",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this.context, "Failed to Generate Address: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }


   /* private suspend fun setQRCode(bitmap: Bitmap){

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
*/
    private fun warningDialogue(){
        val view= View.inflate(this.requireContext(), R.layout.dialog_checkbox, null)
         checkBox = view.findViewById(R.id.dialog_checkBox)
        AlertDialog.Builder(this.requireContext())
            .setTitle("\t\t\t\t\t\t\t\t\t\t\tWARNING")
            .setView(view)
            .setMessage("\nThis is a Testnet Wallet! \n\n" +
                    "Do Not Send Mainnet Bitcoin (BTC) to This Address!\n\nWe Are Not Responsible For Lost Funds!")
            .setPositiveButton("OK"){_ , _ ->
                if(checkBox.isChecked) sharedPref.edit().putBoolean("warning", false).apply()
            }
            .show()
    }

}