package com.example.kotlinbitcoinwallet.utils

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.kotlinbitcoinwallet.R
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BlockInfo
import java.text.SimpleDateFormat
import java.util.*

class SyncDialogFragment(private val syncState:MutableLiveData<BitcoinCore.KitState>, private val recentBlock:MutableLiveData<BlockInfo>): DialogFragment() {
    private lateinit var titleTxt:TextView
    private lateinit var syncTxt:TextView
    private lateinit var cBlockTxt:TextView
    private lateinit var dateTxt:TextView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.sync_dialog, container, false)
        titleTxt = root.findViewById(R.id.tv_dialog_title)
        syncTxt = root.findViewById(R.id.tv_dialog_sync)
        cBlockTxt= root.findViewById(R.id.tv_dialog_cBlock)
        dateTxt=root.findViewById(R.id.tv_dialog_date)
        syncState.observe(this.viewLifecycleOwner, Observer { state ->
            when(state){
                is BitcoinCore.KitState.Synced ->{
                    Log.d("btc-kit-dialog","Synced")
                    Toast.makeText(this.requireContext(),"Wallet is Synced",Toast.LENGTH_SHORT).show()
                    syncTxt.text = SpannableStringBuilder("Synced!")
                  //  dismiss()
                }
                is BitcoinCore.KitState.Syncing ->{

                    syncTxt.text = SpannableStringBuilder("Connection-Status: %${"%.2f".format(state.progress * 100)}")
                    Log.d("btc-kit-dialog","${syncState.value}")
                    Log.d("btc-kit-dialog","${syncState.hasActiveObservers()}")
                }
                is BitcoinCore.KitState.ApiSyncing -> {
                    Log.d("btc-kit", "Api Syncing")
                    "api syncing ${state.transactions} txs"
                }
                is BitcoinCore.KitState.NotSynced -> {
                    Log.d("btc-kit", "Wrecked")
                    "not synced ${state.exception.javaClass.simpleName}"
                }
            }

        })
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        recentBlock.observe(this.viewLifecycleOwner, Observer {
            it?.let {
                blockInfo ->  dateTxt.text =SpannableStringBuilder("Block-Date: ${dateFormat.format(Date(blockInfo.timestamp * 1000))}")
                cBlockTxt.text = SpannableStringBuilder("Block-Height: ${blockInfo.height} ")
            }
        })

        Log.d("btc-kit-dialog","${syncState.value}")
        return root
    }
}