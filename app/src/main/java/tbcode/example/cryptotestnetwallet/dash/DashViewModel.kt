package tbcode.example.cryptotestnetwallet.dash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.reactivex.disposables.CompositeDisposable
import tbcode.example.cryptotestnetwallet.utils.CoinKitEnum
import tbcode.example.cryptotestnetwallet.utils.CoinKit

class DashViewModel : ViewModel() {
    //Role: Retrieve balance and transactions
    val transactions = MutableLiveData<List<TransactionInfo>>()
    val balance = MutableLiveData<BalanceInfo>()

    fun getBalance(bitcoinKit:BitcoinKit){
        balance.value = bitcoinKit.balance
    }
    fun getBalance(kitEnum: CoinKitEnum){
        balance.value = kitEnum.getBalance()
    }
    fun getBalance(coinKit: CoinKit){
        balance.value = coinKit.kit.balance
    }
    fun getTransactions(bitcoinKit: BitcoinKit){
      bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
          transactions.value = txList
      }.let{
            CompositeDisposable().add(it)
      }
    }
    fun getTransactions(kitEnum: CoinKitEnum){
        kitEnum.getTransactions().subscribe { txList: List<TransactionInfo> ->
            transactions.value = txList
        }.let{
            CompositeDisposable().add(it)
        }
    }
    fun getTransactions(coinKit: CoinKit){
        coinKit.kit.transactions().subscribe { txList: List<TransactionInfo> ->
            transactions.value = txList
        }.let{
            CompositeDisposable().add(it)
        }
    }
}