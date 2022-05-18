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
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.kotlinbitcoinwallet.NumberFormatHelper
import tbcode.example.kotlinbitcoinwallet.R
import tbcode.example.kotlinbitcoinwallet.utils.KitSyncService

class SendFragment : Fragment(), PopupMenu.OnMenuItemClickListener{



    private lateinit var viewModel: SendViewModel
    private lateinit var sendTxt:EditText
    private lateinit var amountTxt:EditText
    private lateinit var scanBtn:Button
    private lateinit var sendBtn:Button
    private lateinit var maxBtn:Button
    private lateinit var cryptoKit: BitcoinKit
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
        maxBtn = root.findViewById(R.id.btn_max)






        return root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        cryptoKit =  KitSyncService.bitcoinKit
        viewModel = ViewModelProvider(this).get(SendViewModel::class.java)
        feeRate = SendViewModel.FEE_RATE.MED
        feeTxt.text = SpannableStringBuilder(feeRate.name +" " + viewModel.formattedFee +" tBTC")
        sendTxt.text =SpannableStringBuilder( viewModel.sendAddress)
        amountTxt.text = SpannableStringBuilder( viewModel.formatAmount())
        balanceTxt.text = SpannableStringBuilder(" ${balanceTxt.text} ${NumberFormatHelper.cryptoAmountFormat.format(cryptoKit.balance.spendable / 100_000_000.0)} tBTC" )
        scanBtn.setOnClickListener{

            val scanner = IntentIntegrator(this.activity)
            scanQRCode()
        }
        sendBtn.setOnClickListener{

           confirmDialogue()

        }
    maxBtn.setOnClickListener {
        calculateMax(viewModel.sendAddress)
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
           if( viewModel.amount > 0 && !viewModel.generateFee(cryptoKit, feeRate)){
               Toast.makeText(this.context,"Error: ${viewModel.errorMsg}", Toast.LENGTH_SHORT).show()
           }
           feeTxt.text = SpannableStringBuilder("${feeRate.name} ${viewModel.formatFee()}")
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
        var addy = ""
        val regex:Regex
        val mainNetPattern = "(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}\$"
        val testNetPattern = "(tb1|[nm2])[a-zA-HJ-NP-Z0-9]{25,39}\$"
        regex = if(cryptoKit.networkName==BitcoinKit.NetworkType.TestNet.name) Regex(testNetPattern)
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
    private fun calculateMax(address:String?){
        try {
          val max=  cryptoKit.maximumSpendableValue(address, viewModel.getFeeRate(feeRate), viewModel.getPluginData())
            viewModel.amount = max
            amountTxt.text = SpannableStringBuilder(viewModel.formatAmount())
        } catch (e: Exception) {
            when (e) {

                is SendValueErrors.Dust,
                is SendValueErrors.EmptyOutputs -> Toast.makeText(this.context, "You need at least ${e.message} satoshis to make an transaction",Toast.LENGTH_SHORT ).show()
                is AddressFormatException ->Toast.makeText(this.context, "Could not Format Address",Toast.LENGTH_SHORT).show()
                else -> e.message ?: Toast.makeText(this.context,"Maximum could not be calculated", Toast.LENGTH_SHORT).show()
            }
        }
    }
       private fun confirmDialogue(){
         try {
             val sendAddressStr: String = "To: " + viewModel.sendAddress
             val amountStr = "Amount: ${viewModel.formatAmount()}"
             val feeStr = "Fee: ${viewModel.formatFee()}"
             val finalStr = "Final Amount: ${viewModel.formatTotal()}"
             val warningStr = "NOTE: Do not Attempt to send tBTC to regular BTC Wallets!"
             val alertDialog = AlertDialog.Builder(this.requireContext())
                     .setTitle("Confirm Your Request")
                     .setMessage("Check your Transaction Details: \n $sendAddressStr \n $amountStr \n $feeStr \n $finalStr \n $warningStr")
                     .setPositiveButton("SEND") { _, _ ->
                         try {
                             Log.d("TX", "Sending: ${viewModel.amount} sats\nTo: ${viewModel.sendAddress}\nFee: ${viewModel.getFeeRate(feeRate)}(${feeRate.name})")
                             cryptoKit.validateAddress(viewModel.sendAddress, mutableMapOf())
                           val tx =  cryptoKit.send(viewModel.sendAddress,viewModel.amount,feeRate=viewModel.getFeeRate(feeRate),sortType = TransactionDataSortType.Shuffle,pluginData = mutableMapOf<Byte, IPluginData>())
                             Log.d("SF", "Transaction str: $tx")
                             Log.d( "SF","txInfo: ${tx.header.serializedTxInfo}")
                             Log.d( "SF","meta-hash: ${tx.metadata.transactionHash}")
                             Log.d("SF","header-hash: ${tx.header.hash}")
                             Log.d("SF", "Transaction type: ${tx.metadata.type}")

                         //sentDialogue(tx)
                             Toast.makeText(this.requireContext(), "Transaction Sent Check your Dashboard!", Toast.LENGTH_LONG).show()
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
   private fun sentDialogue(tx: FullTransaction) {
         val alertDialog = AlertDialog.Builder(this.requireContext())
             .setTitle("TRANSACTION SENT!")
             .setMessage("TX-ID:${NumberFormatHelper.cryptoAmountFormat.format( cryptoKit.balance.spendable/ 100_000_000.0)}" +
                     "\nCheck your dash to see your new transaction!")
             .setPositiveButton("OK") { _, _ ->
                 val manager = requireActivity().supportFragmentManager

                 manager.beginTransaction().let {
                     it.detach(this)
                     it.commit()
                 }
                 manager.executePendingTransactions()
                 manager.beginTransaction().let {
                     it.attach(this)
                     it.commit()
                 }


             }.create()
         alertDialog.show()


     }
    private fun feePopup(v:View){
    val feePopup = PopupMenu(context,v)
        feePopup.setOnMenuItemClickListener(this)
        feePopup.inflate(R.menu.fees)
        feePopup.show()
    }
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        feeRate = when(item!!.itemId){
            R.id.high_fee ->{
                SendViewModel.FEE_RATE.HIGH
            }
            R.id.med_fee -> {
                SendViewModel.FEE_RATE.MED
            }
            else -> {
                SendViewModel.FEE_RATE.LOW
            }
        }
        if(!viewModel.generateFee(cryptoKit, feeRate)){
            Toast.makeText(this.context,"Error: ${viewModel.errorMsg}", Toast.LENGTH_SHORT).show()
        }
        feeTxt.text = SpannableStringBuilder("${feeRate.name} ${viewModel.formatFee()}")
        return true
    }






}