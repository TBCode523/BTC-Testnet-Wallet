package tbcode.example.kotlinbitcoinwallet.send

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.kotlinbitcoinwallet.MainActivity
import tbcode.example.kotlinbitcoinwallet.NumberFormatHelper
import tbcode.example.kotlinbitcoinwallet.R

class SendFragment : Fragment(), PopupMenu.OnMenuItemClickListener{



    private lateinit var viewModel: SendViewModel
    private lateinit var sendTxt:EditText
    private lateinit var amountTxt:EditText
    private lateinit var scanBtn:Button
    private lateinit var sendBtn:Button
    private lateinit var bitcoinKit: BitcoinKit
    private lateinit var feeTxt: TextView
    private lateinit var balanceTxt: TextView
    private lateinit var feeRate: SendViewModel.FEE_RATE
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       
        val root = inflater.inflate(R.layout.send_fragment, container, false)
        sendTxt = root.findViewById(R.id.ev_address)
        amountTxt = root.findViewById(R.id.ev_amount)
        feeTxt = root.findViewById(R.id.tv_fee)
        balanceTxt =  root.findViewById(R.id.tv_send_balance)
        scanBtn = root.findViewById(R.id.btn_scan)
        sendBtn = root.findViewById(R.id.btn_send)




        bitcoinKit =  (activity as MainActivity).viewModel.bitcoinKit
        viewModel = ViewModelProvider(this).get(SendViewModel::class.java)
        feeRate = SendViewModel.FEE_RATE.MED


        return root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        feeTxt.text = "${feeRate.name} ${viewModel.formattedFee}"
        sendTxt.text =SpannableStringBuilder( viewModel.sendAddress)
        amountTxt.text = SpannableStringBuilder( viewModel.formatAmount())
        balanceTxt.text = SpannableStringBuilder(" ${balanceTxt.text} ${NumberFormatHelper.cryptoAmountFormat.format(bitcoinKit.balance.spendable / 100_000_000.0)} BTC" )
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
           viewModel.generateFee(bitcoinKit, feeRate)

           feeTxt.text = SpannableStringBuilder("${feeRate.name} ${viewModel.formattedFee}")
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
//TODO Need More Error Checking
             val sendAddressStr: String = "To: " + viewModel.sendAddress
             val amountStr = "Amount: ${viewModel.formatAmount()} BTC"
             val feeStr = "Fee: ${viewModel.formatFee()}"
             val finalStr = "Final Amount: ${viewModel.formatTotal()}"
             val alertDialog = AlertDialog.Builder(this.requireContext())
                     .setTitle("Confirm Your Request")
                     .setMessage("Check your Transaction Details: \n $sendAddressStr \n $amountStr \n $feeStr \n $finalStr ")
                     .setPositiveButton("SEND") { _, _ ->
                         try {
                             Log.d("TX", "Sending: ${viewModel.amount} sats\nTo: ${viewModel.sendAddress}\nFee: ${viewModel.getFeeRate(feeRate)}(${feeRate.name})")
                             bitcoinKit.validateAddress(viewModel.sendAddress, mutableMapOf())
                          val tx= bitcoinKit.send(viewModel.sendAddress,viewModel.amount,feeRate=viewModel.getFeeRate(feeRate),sortType = TransactionDataSortType.Shuffle,pluginData = mutableMapOf<Byte, IPluginData>())
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
               feeRate = SendViewModel.FEE_RATE.HIGH
               viewModel.generateFee(bitcoinKit, feeRate)
               feeTxt.text = SpannableStringBuilder("${feeRate.name} ${viewModel.formatFee()}")
               return true
           }
           R.id.low_fee ->{
               feeRate = SendViewModel.FEE_RATE.LOW
               viewModel.generateFee(bitcoinKit, feeRate)
               feeTxt.text = SpannableStringBuilder("${feeRate.name} ${viewModel.formatFee()}")
               return true
           }
           R.id.med_fee -> {
               feeRate = SendViewModel.FEE_RATE.MED
               viewModel.generateFee(bitcoinKit,feeRate)
               feeTxt.text = SpannableStringBuilder("${feeRate.name} ${viewModel.formatFee()}")
                return true
           }
           else -> {
               return false
           }


       }


    }






}