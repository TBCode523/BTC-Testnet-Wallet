package tbcode.example.cryptotestnetwallet.dash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.reactivex.disposables.CompositeDisposable

class DashViewModel : ViewModel() {
    //Role: Retrieve balance and transactions
    val transactions = MutableLiveData<List<TransactionInfo>>()
    val balance = MutableLiveData<BalanceInfo>()

    fun getBalance(bitcoinKit:BitcoinKit){
        balance.value = bitcoinKit.balance
    }

    fun getTransactions(bitcoinKit: BitcoinKit){
      bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
          transactions.value = txList
      }.let{
            CompositeDisposable().add(it)
      }
    }
}