package tbcode.example.cryptotestnetwallet.utils

import android.app.Application
import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.dashkit.DashKit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.crypto.digest.Keccak256
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Single



sealed class CoinKits(val label: String){
    class tBTC(context:Context, words: List<String>, var kit:BitcoinKit? = null) : CoinKits("tBTC"){

        init {
           createKit(context, words)
        }
        override fun createKit(context: Context, words: List<String>){
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
        }

    }
    class tETH(context:Context, words: List<String>, var kit:EthereumKit?) : CoinKits("tETH"){
        private lateinit var signer:Signer
        init {
            createKit(context, words)
        }
        override fun createKit(context: Context, words: List<String>){
            //TODO generate an ethereum public address
            val k = Keccak256()
            kit = EthereumKit.getInstance(
                Application(),
                Address(""),
                Chain.EthereumGoerli,
                RpcSource.ethereumInfuraHttp("projectId", "projectSecret"),
                TransactionSource.ethereumEtherscan("apiKey"),
                walletId
            ).apply {
                start()
            }
            val seed = Mnemonic().toSeed(words, "")
            signer = Signer.getInstance(seed, Chain.Ethereum)
        }

    }

    val walletId = "MyWallet"
    abstract fun createKit(context: Context, words: List<String>)

}
