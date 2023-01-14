package tbcode.example.cryptotestnetwallet.utils

import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single

enum class CoinKitEnum(var kit:AbstractKit?){

    T_BTC(null) {

    override fun createKit(context: Context, words: List<String>): BitcoinKit {
       kit = BitcoinKit(
            context,
            words,
            passphrase = "",
            walletId,
            BitcoinKit.NetworkType.TestNet,
            syncMode = BitcoinCore.SyncMode.Full(),
            purpose = HDWallet.Purpose.BIP84
        ).apply {
            start()
       }
       return kit as BitcoinKit
    }

    init {
        setLabel()
    }
    override fun setLabel() {
        this.label = "tBTC"
    }

    override fun getBalance(): BalanceInfo {
        return kit?.balance ?: BalanceInfo(0,0)
    }
    override fun getTransactions(): Single<List<TransactionInfo>> {
       return kit!!.transactions()
    }
},;

    val walletId = "MyWallet"
    var label = "tBTC"
    abstract fun createKit(context: Context, words: List<String>):AbstractKit
    abstract fun setLabel()
    abstract fun getBalance(): BalanceInfo
    abstract fun getTransactions(): Single<List<TransactionInfo>>

}
