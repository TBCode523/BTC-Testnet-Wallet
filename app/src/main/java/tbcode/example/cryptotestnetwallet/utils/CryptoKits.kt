package tbcode.example.cryptotestnetwallet.utils

import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hdwalletkit.HDWallet

enum class CryptoKits {
T_BTC {
    lateinit var bitcoinKit: BitcoinKit
    override fun createKit(context: Context, words: List<String>): BitcoinKit {
        bitcoinKit = BitcoinKit(
            context,
            words,
            passphrase = "",
            walletId,
            BitcoinKit.NetworkType.TestNet,
            syncMode = BitcoinCore.SyncMode.Full(),
            purpose = HDWallet.Purpose.BIP84
        )
        return bitcoinKit
    }
    init {
        setLabel()
    }
    override fun setLabel() {
        this.label = "tBTC"
    }

};
    val walletId = "MyWallet"
    var label = "tBTC"
    abstract fun createKit(context: Context, words: List<String>):AbstractKit
    abstract fun setLabel()

}
sealed class CryptoKitsE {
    object T_BTC: CryptoKitsE() {
        lateinit var bitcoinKit: BitcoinKit
        override fun createKit(context: Context, words: List<String>): BitcoinKit {
            bitcoinKit = BitcoinKit(
                context,
                words,
                passphrase = "",
                walletId,
                BitcoinKit.NetworkType.TestNet,
                syncMode = BitcoinCore.SyncMode.Full(),
                purpose = HDWallet.Purpose.BIP84
            )
            return bitcoinKit
        }
        init {
            setLabel()
        }
        override fun setLabel() {
            this.label = "tBTC"
        }

    };
    val walletId = "MyWallet"
    var label = "tBTC"
    abstract fun createKit(context: Context, words: List<String>):AbstractKit
    abstract fun setLabel()

}