package tbcode.example.kotlinbitcoinwallet.dash

import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionStatus
import tbcode.example.kotlinbitcoinwallet.NumberFormatHelper
import tbcode.example.kotlinbitcoinwallet.R
import java.text.DateFormat
import java.util.*

class TxAdapter(var transactions: List<TransactionInfo>?, private val blockInfo: BlockInfo?, private val label: String) : RecyclerView.Adapter<TxAdapter.ViewHolder>()
{
    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val amount: TextView = itemView.findViewById(R.id.tv_amount)
        val img:ImageView = itemView.findViewById(R.id.img_receive_send)

        init {
            itemView.setOnClickListener {

                val txID = transactions?.get(adapterPosition)?.transactionHash
                Log.d("DF", "Transaction indx: $adapterPosition\n Hash: $txID " +
                        "time:${transactions?.get(adapterPosition)?.transactionHash} " +
                        "Height:${transactions?.get(adapterPosition)?.blockHeight}  ")
                 val uriStr = "https://mempool.space/testnet/tx/"
                 val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr+txID))
                Toast.makeText(
                    itemView.context,
                    "Opening block explorer for transaction: $txID",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(itemView.context,intent, null)

            }
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val v = from(parent.context).inflate(R.layout.tx_layout, parent, false)
        return ViewHolder(v)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val amount = transactions?.get(position)?.let { calculateAmount(it) }!!
        val date = transactions?.get(position)?.timestamp?.let { formatDate(it) }
        transactions?.get(position)?.status?.let { Log.d("btc-tx", it.name) }
        when (transactions?.get(position)?.status) {
            TransactionStatus.NEW ->{
                holder.date.text = SpannableStringBuilder("$date (Pending)")
            }
            TransactionStatus.RELAYED -> {
                    if(transactions?.get(position)?.blockHeight == null){
                        holder.date.text = SpannableStringBuilder("$date (Pending)")
                    }
                    else{
                        val diff = blockInfo?.height!! - transactions?.get(position)?.blockHeight!!+1
                        if ( diff < 6 )
                            holder.date.text = SpannableStringBuilder("$date ($diff/6 Confirmations)")
                        else holder.date.text = date
                    }
            }
            else -> holder.date.text = SpannableStringBuilder("Invalid")
        }

        holder.amount.text = SpannableStringBuilder("${NumberFormatHelper.cryptoAmountFormat.format(amount / 100_000_000.0)} $label")

        if(amount > 0){holder.img.setImageResource(R.drawable.ic_receive)}
        else holder.img.setImageResource(R.drawable.ic_send)
    }

    override fun getItemCount(): Int {
        return if ( transactions == null) 0
        else transactions!!.size
    }
    private fun calculateAmount(transactionInfo: TransactionInfo): Long {
        var myInputsTotalValue = 0L

        transactionInfo.inputs.forEach { input ->
            if (input.mine) {
                myInputsTotalValue += input.value ?: 0
            }
        }

        var myOutputsTotalValue = 0L

        transactionInfo.outputs.forEach {
            myOutputsTotalValue += if (it.mine && it.address != null) it.value else 0
        }

        return (myOutputsTotalValue - myInputsTotalValue + (transactionInfo.fee ?: 0))
    }

    private fun formatDate(timestamp: Long): String {

        return DateFormat.getInstance().format(Date(timestamp * 1000))
    }

}