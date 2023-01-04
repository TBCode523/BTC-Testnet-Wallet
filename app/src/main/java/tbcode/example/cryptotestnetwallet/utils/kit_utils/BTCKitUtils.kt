package tbcode.example.cryptotestnetwallet.utils.kit_utils

import android.content.Context
import io.horizontalsystems.bitcoincore.BitcoinCore

import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hdwalletkit.HDWallet

object BTCKitUtils {
        private const val walletId = "MyWallet"
        val networkType = BitcoinKit.NetworkType.TestNet
        val syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api()
        val purpose = HDWallet.Purpose.BIP84
        fun getWalletID() = walletId

}