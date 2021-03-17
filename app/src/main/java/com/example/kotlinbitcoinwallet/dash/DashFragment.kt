package com.example.kotlinbitcoinwallet.dash

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinbitcoinwallet.MainActivity
import com.example.kotlinbitcoinwallet.R
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoinkit.BitcoinKit
//import org.bitcoinj.core.SegwitAddress
//import org.bitcoinj.wallet.Wallet

class DashFragment : Fragment() {



    private lateinit var viewModel: DashViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtBalance: TextView
    private lateinit var txtNoTransaction:TextView
    private lateinit var bitcoinKit: BitcoinKit
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        populateAdapter()
        val root = inflater.inflate(R.layout.dash_fragment, container, false)
        recyclerView = root.findViewById(R.id.dash_recyclerview)
        txtBalance = root.findViewById(R.id.tv_Balance)
        txtNoTransaction = root.findViewById(R.id.tv_NoTransaction)
        txtNoTransaction.visibility = View.GONE
        try {
         //   wallet = (activity as MainActivity).walletAppKit.wallet()
         bitcoinKit =  (activity as MainActivity).viewModel.bitcoinKit

        }catch (e:Exception){
            Toast.makeText(context,"Wallet is Null", Toast.LENGTH_LONG).show()
        }
        return root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DashViewModel::class.java)

        //TODO keep a reference to the kits wallet even after destruction, then add listeners
        viewModel.balance.observe(viewLifecycleOwner, Observer {
            balance ->
            when(balance){
                null ->  txtBalance.text = SpannableStringBuilder("0 BTC: wallet can't be found")
                else-> txtBalance.text = SpannableStringBuilder("${balance.spendable} BTC")
            }
        })


        val txList =viewModel.transactions.value
        val adapter = TxAdapter(txList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this.requireContext())

        if(txList?.isEmpty() == true) txtNoTransaction.visibility = View.VISIBLE


    }
    private fun populateAdapter(): MutableList<String>{
         val stringList = mutableListOf<String>()
        for (i in 1..35){
            stringList.add("TX ")
        }

        return stringList
    }
 /*   private fun showAllAddresses(wallet:Wallet){
        val addresses = wallet.issuedReceiveAddresses
        var str = "Balance: ${wallet.balance}"
        for (address in addresses){
            str+="\n"+ (address as SegwitAddress).toBech32()
        }
        for(t in wallet.walletTransactions!!) {
            str += "\n" + t.transaction.txId
        }
        str+= "\nRecieved: ${wallet.totalReceived.toPlainString()} BTC\n${wallet.totalSent.toPlainString()} BTC \n Creation-Time:${wallet.earliestKeyCreationTime}"
        val alertDialog = AlertDialog.Builder(this.requireContext())
                .setTitle("Wallet Check-Up")
                .setMessage(str)
                .setPositiveButton("OK"){ _, _->






                }.create()
        alertDialog.show()
    }*/
}