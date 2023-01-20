package tbcode.example.cryptotestnetwallet.send

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
import androidx.core.text.isDigitsOnly
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
import tbcode.example.cryptotestnetwallet.NumberFormatHelper
import tbcode.example.cryptotestnetwallet.R
import tbcode.example.cryptotestnetwallet.receive.ReceiveFragment
import tbcode.example.cryptotestnetwallet.utils.CoinKit
import tbcode.example.cryptotestnetwallet.utils.KitSyncService

class SendFragment : Fragment(), PopupMenu.OnMenuItemClickListener{
    private lateinit var viewModel: SendViewModel
    private lateinit var addrTxt:EditText
    private lateinit var amountTxt:EditText
    private lateinit var scanBtn:Button
    private lateinit var sendBtn:Button
    private lateinit var maxSw:SwitchCompat
    private lateinit var cryptoKit: BitcoinKit
    private lateinit var coinKit: CoinKit
    private lateinit var feeTxt: TextView
    private lateinit var balanceTxt: TextView
    private lateinit var feeRate: SendViewModel.FEE_RATE
    companion object{
        const val TAG = "CT-SF"
    }
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
        viewModel = ViewModelProvider(this)[SendViewModel::class.java]
        KitSyncService.isKitAvailable.observe(viewLifecycleOwner){
            if (it){
                Log.d(ReceiveFragment.TAG, "kit is available: ${KitSyncService.bitcoinKit}")
                coinKit = KitSyncService.coinKit!!
                setUpUI()
            }
            else{
                Toast.makeText(context, "Your wallet is not ready yet", Toast.LENGTH_SHORT).show()
            }
        }
        return root
    }
    private fun setUpUI(){
        //cryptoKit =  KitSyncService.bitcoinKit!!
        Log.d(TAG, "onActivityCreated")
        feeRate = if(viewModel.feeR.value != null){
            Log.d(TAG, "SVM fee rate: ${viewModel.feeR.value!!}")
            viewModel.feeR.value!!
        } else{
            Log.d(TAG, "SVM fee rate is null")
            SendViewModel.FEE_RATE.HIGH
        }
        Log.d(TAG, "feeRate: $feeRate")
        feeTxt.text = SpannableStringBuilder(feeRate.name +" "+ viewModel.formatFee())
        amountTxt.text = SpannableStringBuilder(viewModel.formatAmount(0L))
        balanceTxt.text = SpannableStringBuilder(" ${balanceTxt.text} ${NumberFormatHelper.cryptoAmountFormat.format(coinKit.kit.balance.spendable / 100_000_000.0)} ${coinKit.label}" )
        scanBtn.setOnClickListener{
            val scanner = IntentIntegrator(this.activity)
            scanQRCode()
        }
        sendBtn.setOnClickListener{
            confirmDialogue()
        }

        maxSw.setOnClickListener {
            val sendAddr = addrTxt.text.toString()
            if(maxSw.isChecked){
                if(sendAddr.isNotBlank() && sendAddr.isNotEmpty()) {
                    calculateMax(sendAddr)
                }
                else{
                    maxSw.isChecked = false
                    Toast.makeText(this.activity, "Address field is blank", Toast.LENGTH_LONG).show()
                }
            }
        }
        feeTxt.setOnClickListener{
            feePopup(feeTxt)
        }
        addrTxt.doOnTextChanged { text, _, _, _ ->
            //viewModel.sendAddress = text.toString()
            Log.d(TAG, "Addr changed to: $text")
            if (addrTxt.text.isEmpty() || addrTxt.text.isBlank()){
                maxSw.isChecked = false
            }
        }
        amountTxt.doOnTextChanged { text, _, _, _ ->
            var newAmount = 0L
            if(text.toString().toDoubleOrNull() != null){
                newAmount = (text.toString().toDouble() * SendViewModel.sats).toLong()
            }
            Log.d(TAG, "Amount changed to: $newAmount")
            try {
                maxSw.isChecked = newAmount == coinKit.kit.maximumSpendableValue(addrTxt.text.toString(), viewModel.getFeeRate(feeRate), mutableMapOf())
            }catch (e:Exception){
                maxSw.isChecked = false
            }
            if( newAmount > 0 && !viewModel.generateFee(coinKit, feeRate, newAmount)){
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
                /*TODO Bugs involving parsePaymentAddress include:
                    -amount field not working for bech32 addresses
                    -invalid addresses can still pass the parsing checks
                 */
                Log.d(TAG, "qrScanner contents: ${result.contents}")
                val parsedQr = coinKit.kit.parsePaymentAddress(result.contents)
                Log.d(TAG, "parsedQr: $parsedQr")
                if(parsedQr.address != "null"){
                    addrTxt.text = SpannableStringBuilder(parsedQr.address)
                }
                else{
                    Toast.makeText(this.context, "Invalid QRCode", Toast.LENGTH_SHORT).show()
                    return
                }
                if(parsedQr.amount == null){
                    amountTxt.text = SpannableStringBuilder("0.00")
                }
                else amountTxt.text = SpannableStringBuilder(parsedQr.amount.toString())
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun calculateMax(address:String?){
        try {
            val max = coinKit.kit.maximumSpendableValue(address, viewModel.getFeeRate(feeRate), viewModel.getPluginData())
            amountTxt.text = SpannableStringBuilder(viewModel.formatAmount(max))
        } catch (e: Exception) {
            when (e) {
                is SendValueErrors.Dust,
                is SendValueErrors.EmptyOutputs -> Toast.makeText(this.context, "You must have at least 0.00001 tBTC",Toast.LENGTH_SHORT ).show()
                is AddressFormatException ->Toast.makeText(this.context, "Could not Format Address",Toast.LENGTH_SHORT).show()
                else -> e.message ?: Toast.makeText(this.context,"Maximum could not be calculated", Toast.LENGTH_SHORT).show()
            }
            maxSw.isChecked = false
        }
    }

    private fun confirmDialogue(){
        try {
            val amount = (amountTxt.text.toString().toDouble() * SendViewModel.sats).toLong()
            val sendAddress = addrTxt.text.toString()
            val sendAddressStr = "To: $sendAddress"
            val amountStr = "Amount: ${viewModel.formatAmount(amount)}"
            val feeStr = "Fee: ${viewModel.formatFee()}"
            val finalStr = "Final Amount: ${viewModel.formatTotal(amount)}"
            val warningStr = "NOTE: Do not Attempt to send tBTC to regular BTC Wallets!"
            val alertDialog = AlertDialog.Builder(this.requireContext())
                .setTitle("Confirm Your Request")
                .setMessage("Check your Transaction Details: \n $sendAddressStr \n $amountStr \n $feeStr \n $finalStr \n $warningStr")
                .setPositiveButton("SEND") { _, _ ->
                    try {
                        Log.d("TX", "Sending: $amount sats\nTo: ${sendAddress}\nFee: ${viewModel.getFeeRate(feeRate)}(${feeRate.name})")
                        coinKit.kit.validateAddress(sendAddress, mutableMapOf())
                        val tx =  coinKit.kit.send(sendAddress,amount,feeRate=viewModel.getFeeRate(feeRate),sortType = TransactionDataSortType.Shuffle,pluginData = mutableMapOf<Byte, IPluginData>())
                        Log.d("SF", "Transaction str: $tx")
                        Log.d( "SF","txInfo: ${tx.header.serializedTxInfo}")
                        Log.d( "SF","meta-hash: ${tx.metadata.transactionHash}")
                        Log.d("SF","header-hash: ${tx.header.hash}")
                        Log.d("SF", "Transaction type: ${tx.metadata.type}")
                        addrTxt.text.clear()
                        //viewModel.amount = 0
                        amountTxt.text = SpannableStringBuilder(viewModel.formatAmount(0))
                        balanceTxt.text = SpannableStringBuilder("Current Balance: ${NumberFormatHelper.cryptoAmountFormat.format(coinKit.kit.balance.spendable / 100_000_000.0)} tBTC" )
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
        viewModel._feeR.value = feeRate
        if (maxSw.isChecked){
            calculateMax(addrTxt.text.toString())
        }
        else{
            val amount = (amountTxt.text.toString().toDouble() * SendViewModel.sats).toLong()
            if(!viewModel.generateFee(coinKit, feeRate, amount)){
                Toast.makeText(this.context,"Error: ${viewModel.errorMsg}", Toast.LENGTH_SHORT).show()
            }
        }
        feeTxt.text = SpannableStringBuilder("${feeRate.name} ${viewModel.formatFee()}")
        return true
    }

}