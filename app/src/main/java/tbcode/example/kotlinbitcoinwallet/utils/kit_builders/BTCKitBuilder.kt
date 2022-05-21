package tbcode.example.kotlinbitcoinwallet.utils.kit_builders

import android.content.Context
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoinkit.BitcoinKit

object BTCKitBuilder {

        const val walletId = "MyWallet"
        var networkType = BitcoinKit.NetworkType.TestNet
        var syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api()
        var bip = Bip.BIP84
        fun createKit(context: Context, words: List<String>): BitcoinKit{
            return BitcoinKit(context, words,  "",walletId,
                networkType, syncMode = syncMode, bip = bip)

    }

}