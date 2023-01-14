package tbcode.example.cryptotestnetwallet.dash

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.cryptotestnetwallet.MainActivity
import tbcode.example.cryptotestnetwallet.NumberFormatHelper
import tbcode.example.cryptotestnetwallet.R
import tbcode.example.cryptotestnetwallet.utils.CoinKitEnum
import tbcode.example.cryptotestnetwallet.utils.CoinKits
import tbcode.example.cryptotestnetwallet.utils.KitSyncService



class DashFragment : Fragment(){
    private lateinit var viewModel: DashViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtBalance: TextView
    private lateinit var txtNoTransaction:TextView
    private lateinit var cryptoKit: BitcoinKit
    private lateinit var adapter: TxAdapter
    companion object{
        const val TAG = "CT-DF"
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[DashViewModel::class.java]
        val root = inflater.inflate(R.layout.dash_fragment, container, false)
        recyclerView = root.findViewById(R.id.dash_recyclerview)
        txtBalance = root.findViewById(R.id.tv_Balance)
        txtNoTransaction = root.findViewById(R.id.tv_NoTransaction)
        KitSyncService.isKitAvailable.observe(viewLifecycleOwner){
            if(it){
                Log.d(TAG, "kit is available!")
                txtNoTransaction.visibility = View.GONE
                setUpUI()
            }
            else{
                txtBalance.text = SpannableStringBuilder("0.00 tBTC")
                Log.d(TAG, "kit not available")
                txtNoTransaction.visibility = View.VISIBLE
                txtNoTransaction.text = "Loading..."
            }
        }
        return root
    }
    private fun setUpUI(){
        if(KitSyncService.isRunning) Log.d(TAG, "KitSyncService is Running")
        else  Log.d(TAG, "KitSyncService is not Running")
        when(KitSyncService.coinKitEnum){
            CoinKitEnum.T_BTC ->{
                cryptoKit = CoinKitEnum.T_BTC.kit as BitcoinKit
            }
        }
        cryptoKit = KitSyncService.bitcoinKit!!
        viewModel.getBalance(cryptoKit)
        viewModel.getTransactions(cryptoKit)
        adapter = TxAdapter(viewModel.transactions.value, cryptoKit.lastBlockInfo, "tBTC")
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        Log.d(TAG, "Unspendable: ${cryptoKit.balance.unspendable}")
        Log.d(TAG, "Spendable: ${cryptoKit.balance.spendable}")
        Log.d(TAG, "Block-Height: ${cryptoKit.lastBlockInfo?.height}")
        Log.d(TAG, "Unspendable + Spendable: ${cryptoKit.balance.spendable + cryptoKit.balance.unspendable}")
        viewModel.balance.observe(viewLifecycleOwner) { balance ->
            when (balance) {
                null -> txtBalance.text =
                    SpannableStringBuilder("0 tBTC: wallet can't be found")
                else -> txtBalance.text = SpannableStringBuilder(
                    "${
                        NumberFormatHelper.cryptoAmountFormat.format(balance.spendable / 100_000_000.0)
                    } tBTC"
                )
            }
        }
        viewModel.transactions.observe(viewLifecycleOwner) {
            if(it.isEmpty()) txtNoTransaction.visibility = View.VISIBLE
            else {
                adapter.transactions = it
                adapter.notifyDataSetChanged()
            }
        }


    }
}