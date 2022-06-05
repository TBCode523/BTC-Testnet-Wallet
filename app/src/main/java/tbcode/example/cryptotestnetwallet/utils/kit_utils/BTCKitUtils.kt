package tbcode.example.cryptotestnetwallet.utils.kit_utils

import android.content.Context
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoinkit.BitcoinKit

object BTCKitUtils {
        private const val walletId = "MyWallet"
        val networkType = BitcoinKit.NetworkType.TestNet
        val syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api()
        val bip = Bip.BIP84
        fun getWalletID() = walletId

}