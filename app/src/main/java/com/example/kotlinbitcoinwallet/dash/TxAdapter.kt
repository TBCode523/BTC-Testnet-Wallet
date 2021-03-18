package com.example.kotlinbitcoinwallet.dash

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater.*
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.kotlinbitcoinwallet.R
import io.horizontalsystems.bitcoincore.models.TransactionInfo

class TxAdapter( var transactions: List<TransactionInfo>?) : RecyclerView.Adapter<TxAdapter.ViewHolder>()
{
    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        //TODO add more textViews regarding tx info
        val itemTitle: TextView = itemView.findViewById(R.id.tv_item)


        init {
            itemView.setOnClickListener {
                val position: Int = adapterPosition
                Toast.makeText(
                    itemView.context,
                    "You have clicked on item #:${position + 1} ",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxAdapter.ViewHolder {

        val v = from(parent.context).inflate(R.layout.tx_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: TxAdapter.ViewHolder, position: Int) {
       holder.itemTitle.text = "${transactions?.get(position)?.transactionHash}   ${position+1}"
    }

    override fun getItemCount(): Int {
        return if ( transactions == null) 0
        else transactions!!.size
    }



}