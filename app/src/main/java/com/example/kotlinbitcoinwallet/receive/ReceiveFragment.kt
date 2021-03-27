package com.example.kotlinbitcoinwallet.receive
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.kotlinbitcoinwallet.MainActivity
import com.example.kotlinbitcoinwallet.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import io.horizontalsystems.bitcoinkit.BitcoinKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceiveFragment : Fragment() {



    private lateinit var viewModel: ReceiveViewModel
    private lateinit var receiveTxt: TextView
    private lateinit var qrCode: ImageView
    private lateinit var generateBtn:Button
    private lateinit var bitcoinKit: BitcoinKit
    private companion object{var walletNum: Int = 0}
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.receive_fragment, container, false)
         receiveTxt = root.findViewById(R.id.tv_Receive)

         qrCode= root.findViewById(R.id.qr_code)
         generateBtn = root.findViewById(R.id.generate_btn)
        viewModel = ViewModelProvider(this).get(ReceiveViewModel::class.java)
        bitcoinKit =  (activity as MainActivity).viewModel.bitcoinKit
        try {
            receiveClick()
        }catch (e:Exception){
            Toast.makeText(context,"Wallet is Null",Toast.LENGTH_LONG).show()
        }
        generateBtn.setOnClickListener {
            receiveClick()
            }

        return root
    }

    private fun receiveClick() {
        try {
            val generatedAddress: String = viewModel.generateAddress(bitcoinKit)
            qrCode.setImageBitmap(null)
            Toast.makeText(this.context, "Generating QRCode", Toast.LENGTH_SHORT).show()



            CoroutineScope(IO).launch {

                generateQRCode(generatedAddress)
            }

            receiveTxt.text = generatedAddress

            Log.v("Wallet", "Creating Wallet $walletNum")
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


}