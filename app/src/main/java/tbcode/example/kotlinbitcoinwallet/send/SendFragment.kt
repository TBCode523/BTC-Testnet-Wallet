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
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.kotlinbitcoinwallet.NumberFormatHelper
import tbcode.example.kotlinbitcoinwallet.R
import tbcode.example.kotlinbitcoinwallet.utils.KitSyncService

class SendFragment : Fragment(), PopupMenu.OnMenuItemClickListener{



    private lateinit var viewModel: SendViewModel
    private lateinit var addrTxt:EditText
    private lateinit var amountTxt:EditText
    private lateinit var scanBtn:Button
    private lateinit var sendBtn:Button
    private lateinit var maxSw:SwitchCompat
    private lateinit var cryptoKit: BitcoinKit
    private lateinit var feeTxt: TextView
    private lateinit var balanceTxt: TextView
    private lateinit var feeRate: SendViewModel.FEE_RATE
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       
        val root = inflater.inflate(R.layout.send_fragment, container, false)
        addrTxt = root.findViewById(R.id.ev_address)
        amountTxt = root.findViewById(R.id.ev_amount)
        feeTxt = root.findViewById(R.id.tv_fee)
        balanceTxt =  root.findViewById(R.id.tv_send_balance)
        scanBtn = root.findViewById(R.id.btn_scan)
        sendBtn = root.findViewById(R.id.btn_send)
        maxSw = root.findViewById(R.id.btn_max)
        viewModel = ViewModelProvider(this).get(SendViewModel::class.java)
        return root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        cryptoKit =  KitSyncService.bitcoinKit

        feeRate = SendViewModel.FEE_RATE.HIGH
        feeTxt.text = SpannableStringBuilder(feeRate.name +" " + viewModel.formattedFee +" tBTC")
        addrTxt.text =SpannableStringBuilder( viewModel.sendAddress)
        amountTxt.text = SpannableStringBuilder( viewModel.formatAmount())
        balanceTxt.text = SpannableStringBuilder(" ${balanceTxt.text} ${NumberFormatHelper.cryptoAmountFormat.format(cryptoKit.balance.spendable / 100_000_000.0)} tBTC" )
        Log.d("SF", "addrViewmodel: ${viewModel.sendAddress}")
        scanBtn.setOnClickListener{

            val scanner = IntentIntegrator(this.activity)
            scanQRCode()
        }
        sendBtn.setOnClickListener{

           confirmDialogue()

        }
        maxSw.setOnCheckedChangeListener { _, is_On ->
               if(is_On)
                   calculateMax(viewModel.sendAddress)
        }
        feeTxt.setOnClickListener{
            feePopup(feeTxt)
        }

        addrTxt.doOnTextChanged { text, _, _, _ ->
            viewModel.sendAddress = text.toString()
            Log.d("SF", "Addr changed to: ${viewModel.sendAddress}")
        }

        amountTxt.doOnTextChanged { text, _, _, _ ->
           if( text!!.isNotBlank()){
               viewModel.amount = (text.toString().toDouble() * viewModel.sats).toLong()
            }

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
                val parsedQr = parseQR(result.contents)
                addrTxt.text = SpannableStringBuilder(parsedQr[0])
                amountTxt.text = SpannableStringBuilder(parsedQr[1])
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun parseQR(contents: String): List<String> {
        var addy = ""
        var amm = ""
        val addyRegex:Regex
        val ammRegex: Regex
        val testNetPattern = "(tb1|[nm2])[a-zA-HJ-NP-Z0-9]{25,39}\$"
        val ammPattern = "[1-9]\\d*(\\.\\d+)?\$"
        addyRegex = Regex(testNetPattern)
        ammRegex = Regex(ammPattern)


        if(addyRegex.containsMatchIn(contents)){
           var match = addyRegex.find(contents)
            addy =  match!!.value
            if (ammRegex.containsMatchIn(contents)){
                match = ammRegex.find(contents)
                amm = match!!.value
            }
            Toast.makeText(this.activity, "Scanned: $contents", Toast.LENGTH_LONG).show()


        }
        else{
            Toast.makeText(this.activity, "Invalid Address: $contents", Toast.LENGTH_LONG).show()
        }

        return listOf(addy,amm)
    }
    private fun calculateMax(address:String?){
        try {
          val max=  cryptoKit.maximumSpendableValue(address, viewModel.getFeeRate(feeRate), viewModel.getPluginData())
            viewModel.amount = max
            amountTxt.text = SpannableStringBuilder(viewModel.formatAmount())
        } catch (e: Exception) {
            when (e) {

                is SendValueErrors.Dust,
                is SendValueErrors.EmptyOutputs -> Toast.makeText(this.context, "You need at least 0. satoshis to make an transaction",Toast.LENGTH_SHORT ).show()
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
                             addrTxt.text.clear()
                             viewModel.amount = 0
                             amountTxt.text = SpannableStringBuilder(viewModel.formatAmount())
                             balanceTxt.text = SpannableStringBuilder("Current Balance: ${NumberFormatHelper.cryptoAmountFormat.format(cryptoKit.balance.spendable / 100_000_000.0)} tBTC" )
                             maxSw.isChecked = false
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
        if (maxSw.isChecked){
            calculateMax(viewModel.sendAddress)
        }
        else{
            if(!viewModel.generateFee(cryptoKit, feeRate)){
                Toast.makeText(this.context,"Error: ${viewModel.errorMsg}", Toast.LENGTH_SHORT).show()
            }
        }

        feeTxt.text = SpannableStringBuilder("${feeRate.name} ${viewModel.formatFee()}")
        return true
    }






}