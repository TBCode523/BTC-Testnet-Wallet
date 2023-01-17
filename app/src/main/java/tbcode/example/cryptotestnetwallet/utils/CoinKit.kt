package tbcode.example.cryptotestnetwallet.utils

import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.litecoinkit.LitecoinKit


sealed class CoinKit(val label: String){
    lateinit var kit: AbstractKit
    class tBTC(context:Context, words: List<String>) : CoinKit("tBTC"){

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
            )
        }

    }
    class tLTC(context:Context, words: List<String>) : CoinKit("tLTC"){

        init {
            createKit(context, words)
        }
        override fun createKit(context: Context, words: List<String>){
            kit = LitecoinKit(
                context,
                words,
                passphrase = "",
                walletId,
                LitecoinKit.NetworkType.TestNet,
                syncMode = BitcoinCore.SyncMode.Full(),
                purpose = HDWallet.Purpose.BIP84
            )
        }

    }
    /*class tETH(context:Context, words: List<String>, var kit:EthereumKit?) : CoinKit("tETH"){
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

    }*/

    val walletId = "MyWallet"
    abstract fun createKit(context: Context, words: List<String>)

}
