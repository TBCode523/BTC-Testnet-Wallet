package com.example.kotlinbitcoinwallet.send

import FeePriority
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.example.kotlinbitcoinwallet.MainActivity
import com.example.kotlinbitcoinwallet.NumberFormatHelper
import com.example.kotlinbitcoinwallet.R
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoinkit.BitcoinKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.net.URL

class SendFragment : Fragment(), PopupMenu.OnMenuItemClickListener{



    private lateinit var viewModel: SendViewModel
    private lateinit var sendTxt:EditText
    private lateinit var amountTxt:EditText
    private lateinit var scanBtn:Button
    private lateinit var sendBtn:Button
    private lateinit var bitcoinKit: BitcoinKit
    private lateinit var feeTxt: TextView
    private lateinit var txIDTxt:TextView
    private lateinit var feePriority: FeePriority
    private lateinit var fee_Rate: SendViewModel.FEE_RATE
    private  var feeRate:Int = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       
        val root = inflater.inflate(R.layout.send_fragment, container, false)
        sendTxt = root.findViewById(R.id.ev_address)
        amountTxt = root.findViewById(R.id.ev_amount)
        feeTxt = root.findViewById(R.id.tv_fee)
        scanBtn = root.findViewById(R.id.btn_scan)
        sendBtn = root.findViewById(R.id.btn_send)
        txIDTxt = root.findViewById(R.id.tv_txID)



        bitcoinKit =  (activity as MainActivity).viewModel.bitcoinKit
        viewModel = ViewModelProvider(this).get(SendViewModel::class.java)
        fee_Rate = SendViewModel.FEE_RATE.MED
        CoroutineScope(IO).launch {

            feePriority = generateFeePriority("https://mempool.space/api/v1/fees/recommended")
            feeRate = feeRate

        }

        return root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        feeTxt.text = "${fee_Rate.name} ${viewModel.formattedFee}"
        sendTxt.text =SpannableStringBuilder( viewModel.sendAddress)
        amountTxt.text = SpannableStringBuilder( viewModel.formatAmount())


        scanBtn.setOnClickListener{

            val scanner = IntentIntegrator(this.activity)
            scanQRCode()
        }
        sendBtn.setOnClickListener{

           confirmDialogue()

        }

    feeTxt.setOnClickListener{
        feePopup(feeTxt)
    }
        sendTxt.doOnTextChanged { text, _, _, _ -> viewModel.sendAddress =
            text.toString()
        }
       amountTxt.doOnTextChanged { text, _, _, _ ->
           viewModel.amount = (text.toString().toDouble() * viewModel.sats).toLong()
           Log.d("SF", "Amount changed to: ${viewModel.amount}")
           viewModel.generateFee(bitcoinKit, fee_Rate)

           feeTxt.text = "${fee_Rate.name} ${viewModel.formattedFee}"
       }

    }
    private fun scanQRCode(){
        val integrator = IntentIntegrator.forSupportFragment(this).apply {
            captureActivity = CaptureActivity::class.java
            setOrientationLocked(false)
            setBeepEnabled(false)
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            setPrompt("Scanning Address")
        }
        Log.e("Scanner","Scanning")
        integrator.initiateScan()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) Toast.makeText(this.activity, "Cancelled", Toast.LENGTH_LONG).show()
            else {


                sendTxt.text = SpannableStringBuilder(parseQR(result.contents))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun parseQR(contents: String): String {
        var addy:String = ""
        val regex:Regex
        //TODO Accommodate with qr codes that don't start with the address ^ means starting with
        val mainNetPattern = "(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}\$"
        val testNetPattern = "(tb1|[nm2])[a-zA-HJ-NP-Z0-9]{25,39}\$"
        regex = if(bitcoinKit.networkName==BitcoinKit.NetworkType.TestNet.name) Regex(testNetPattern)
        else Regex(mainNetPattern)


        if(regex.containsMatchIn(contents)){
           val match = regex.find(contents)
            addy =  match!!.value
            Toast.makeText(this.activity, "Scanned: $contents", Toast.LENGTH_LONG).show()


        }
        else{
            Toast.makeText(this.activity, "Invalid Address: $contents", Toast.LENGTH_LONG).show()
        }
        return addy
    }
       private fun confirmDialogue(){
         try {
          /*   val sats = 100000000
             val sendAddress = StringBuilder(sendTxt.text).toString()
             val amount = StringBuilder(amountTxt.text).toString()
             val amountToLong = (amount.toDouble() * sats).toLong()
             val fee = generateFee(amountToLong)
             */
            // val amountFormatted = NumberFormatHelper.cryptoAmountFormat.format(amount / 100_000_000.0)

             val sendAddressStr: String = "To: " + viewModel.sendAddress
             val amountStr = "Amount: ${viewModel.formatAmount()} BTC"
             val feeStr = "Fee: ${viewModel.formatFee()}"
             val finalStr = "Final Amount: ${viewModel.formatTotal()}"
             val alertDialog = AlertDialog.Builder(this.requireContext())
                     .setTitle("Confirm Your Request")
                     .setMessage("Check your Transaction Details: \n $sendAddressStr \n $amountStr \n $feeStr \n $finalStr ")
                     .setPositiveButton("SEND") { _, _ ->
                         try {
                             Log.d("TX", "Sending: ${viewModel.amount} sats\nTo: ${viewModel.sendAddress}\nFee: ${viewModel.getFeeRate(fee_Rate)}(${fee_Rate.name})")
                          val tx= bitcoinKit.send(viewModel.sendAddress,viewModel.amount,feeRate=viewModel.getFeeRate(fee_Rate),sortType = TransactionDataSortType.Shuffle,pluginData = mutableMapOf<Byte, IPluginData>())
                            sentDialogue(tx.header.hash)
                         } catch (e:SendValueErrors.Dust){
                             Toast.makeText(this.requireContext(), "You need at least: ${e.message}", Toast.LENGTH_LONG).show()
                         } catch (e:Exception){
                             Toast.makeText(this.requireContext(), "Transaction Request Failed: ${e.message}", Toast.LENGTH_LONG).show()

                         }


                     }
                     .setNegativeButton("CANCEL") { _, _ ->

                     }.create()
             alertDialog.show()
         }catch (e:Exception){
             Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
         }
     }
   private fun sentDialogue(txInfo: ByteArray){
         val alertDialog = AlertDialog.Builder(this.requireContext())
             .setTitle("TRANSACTION SENT!")
             .setMessage("New Balance:${NumberFormatHelper.cryptoAmountFormat.format( bitcoinKit.balance.spendable/ 100_000_000.0)}" +
                     "\nCheck your dash to see your new transaction!")
             .setPositiveButton("OK") { _, _ ->

               /*  try {
                     val uriStr = "https://mempool.space/testnet/tx/"
                     val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr+txInfo))
                     startActivity(intent)
                 } catch (e:Exception){
                     Toast.makeText(this.requireContext(), "Transaction Request Failed", Toast.LENGTH_SHORT).show()
                 }
                    */

             }.create()
           /*  .setNegativeButton("COPY TX-ID") { _, _ ->
                 val clipBoard = this.activity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
               clipBoard.setPrimaryClip( ClipData.newPlainText("TX-ID", txInfo))
             }.create()*/
         alertDialog.show()


     }
    private fun feePopup(v:View){
    val feePopup = PopupMenu(context,v)
        feePopup.setOnMenuItemClickListener(this)
        feePopup.inflate(R.menu.fees)
        feePopup.show()
    }
    override fun onMenuItemClick(item: MenuItem?): Boolean {

       when(item!!.itemId){
           R.id.high_fee ->{
               fee_Rate = SendViewModel.FEE_RATE.HIGH
               viewModel.generateFee(bitcoinKit, fee_Rate)
               feeTxt.text = SpannableStringBuilder("${fee_Rate.name} ${viewModel.formatFee()}")
               return true
           }
           R.id.low_fee ->{
               fee_Rate = SendViewModel.FEE_RATE.LOW
               viewModel.generateFee(bitcoinKit, fee_Rate)
               feeTxt.text = SpannableStringBuilder("${fee_Rate.name} ${viewModel.formatFee()}")
               return true
           }
           R.id.med_fee -> {
               fee_Rate = SendViewModel.FEE_RATE.MED
               viewModel.generateFee(bitcoinKit,fee_Rate)
               feeTxt.text = SpannableStringBuilder("${fee_Rate.name} ${viewModel.formatFee()}")
                return true
           }
           else -> {
               return false
           }


       }


    }


    private fun generateFeePriority(feeUrl: String): FeePriority {
        val response = URL(feeUrl).readText()
        val gson = Gson()
        return gson.fromJson(response, FeePriority::class.java)
    }
    private fun generateFee(value: Long, address: String? = null): Long {
        return bitcoinKit.fee(value, address, feeRate = feeRate)
    }


}