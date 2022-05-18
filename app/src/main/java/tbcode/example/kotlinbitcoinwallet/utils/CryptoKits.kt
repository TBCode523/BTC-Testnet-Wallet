package tbcode.example.kotlinbitcoinwallet.utils

import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import tbcode.example.kotlinbitcoinwallet.utils.kit_builders.BTCKitBuilder

enum class CryptoKits {
BTC {
    val label = "tBTC"
    override fun createKit(context: Context, words: List<String>): AbstractKit {
        return BTCKitBuilder.createKit(context, words)
    }
};


    abstract fun createKit(context: Context, words: List<String>):AbstractKit

}