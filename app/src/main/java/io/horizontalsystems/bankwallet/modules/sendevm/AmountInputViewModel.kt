package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceNew
import io.horizontalsystems.bankwallet.core.fiat.FiatServiceNew
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import java.lang.Integer.min
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*


interface IAmountInputService {
    val amount: BigDecimal
    val coin: Coin?
    val balance: BigDecimal?

    val amountObservable: Flowable<BigDecimal>
    val coinObservable: Flowable<Optional<Coin>>

    fun onChangeAmount(amount: BigDecimal)
}

class AmountInputViewModel(
        private val service: IAmountInputService,
        private val fiatServiceNew: FiatServiceNew,
        private val switchServiceNew: AmountTypeSwitchServiceNew,
        private val isMaxSupported: Boolean = true
) : ViewModel() {
    private val disposable = CompositeDisposable()

    private var validDecimals = maxValidDecimals

    val prefixLiveData = MutableLiveData<String?>(null)
    val amountLiveData = MutableLiveData<String?>(null)
    val maxEnabledLiveData = MutableLiveData(false)
    val secondaryTextLiveData = MutableLiveData<String?>(null)
    val switchEnabledLiveData = MutableLiveData(false)
    val revertAmountLiveData = MutableLiveData<String>()

    init {
        service.amountObservable.subscribeIO { syncAmount(it) }.let { disposable.add(it) }
        service.coinObservable.subscribeIO { syncCoin(it.orElse(null)) }.let { disposable.add(it) }
        fiatServiceNew.coinAmountObservable.subscribeIO { syncCoinAmount(it) }.let { disposable.add(it) }
        fiatServiceNew.primaryInfoObservable.subscribeIO { syncPrimaryInfo(it) }.let { disposable.add(it) }
        fiatServiceNew.secondaryAmountInfoObservable.subscribeIO { syncSecondaryAmountInfo(it.orElse(null)) }.let { disposable.add(it) }
        switchServiceNew.toggleAvailableObservable.subscribeIO { switchEnabledLiveData.postValue(it) }.let { disposable.add(it) }

        syncAmount(service.amount)
        syncCoin(service.coin)
        syncCoinAmount(fiatServiceNew.coinAmount)
        syncPrimaryInfo(fiatServiceNew.primaryInfo)
        syncSecondaryAmountInfo(fiatServiceNew.secondaryAmountInfo)
    }

    private fun syncAmount(amount: BigDecimal) {
        fiatServiceNew.setCoinAmount(amount)
    }

    private fun syncCoin(coin: Coin?) {
        val max = maxValidDecimals
        validDecimals = min(max, (coin?.decimal ?: max))

        fiatServiceNew.setCoin(coin)

        maxEnabledLiveData.postValue(isMaxSupported &&
                (service.balance ?: BigDecimal.ZERO) > BigDecimal.ZERO)
    }

    private fun syncCoinAmount(amount: BigDecimal) {
        service.onChangeAmount(amount)
    }

    private fun getPrefix(primaryInfo: FiatServiceNew.PrimaryInfo): String? =
            if (primaryInfo is FiatServiceNew.PrimaryInfo.Info) {
                primaryInfo.amountInfo?.let {
                    if (it is AmountInfo.CurrencyValueInfo) {
                        it.currencyValue.currency.symbol
                    } else null
                }
            } else null

    private fun getAmountString(primaryInfo: FiatServiceNew.PrimaryInfo): String? =
            when (primaryInfo) {
                is FiatServiceNew.PrimaryInfo.Info -> {
                    val amountInfo = primaryInfo.amountInfo
                    if (amountInfo == null || amountInfo.value <= BigDecimal.ZERO) {
                        null
                    } else {
                        val amount = amountInfo.value.setScale(min(amountInfo.decimal, maxValidDecimals), RoundingMode.DOWN)
                        amount.stripTrailingZeros().toPlainString()
                    }
                }
                is FiatServiceNew.PrimaryInfo.Amount -> {
                    val amount = primaryInfo.amount.setScale(maxValidDecimals, RoundingMode.DOWN)
                    amount.stripTrailingZeros().toPlainString()
                }
            }

    private fun syncPrimaryInfo(primaryInfo: FiatServiceNew.PrimaryInfo) {
        amountLiveData.postValue(getAmountString(primaryInfo))
        prefixLiveData.postValue(getPrefix(primaryInfo))
    }

    private fun syncSecondaryAmountInfo(amountInfo: AmountInfo?) {
        secondaryTextLiveData.postValue(amountInfo?.getFormatted())
    }

    fun isValid(amount: String?): Boolean {
        val amountDecimal = amount?.toBigDecimalOrNull()
        return amountDecimal != null && amountDecimal.scale() <= validDecimals
    }

    fun areAmountsEqual(lhs: String?, rhs: String?): Boolean {
        val lhsDecimal = lhs?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val rhsDecimal = rhs?.toBigDecimalOrNull() ?: BigDecimal.ZERO

        return lhsDecimal.compareTo(rhsDecimal) == 0
    }

    fun onChangeAmount(amount: String?) {
        val amountDecimal = amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        if (amountDecimal != null && amountDecimal.scale() > validDecimals) {
            val amountNumber = amountDecimal.setScale(validDecimals, RoundingMode.FLOOR)
            val revertedInput = amountNumber.toPlainString()
            revertAmountLiveData.postValue(revertedInput)
        } else {
            fiatServiceNew.setAmount(amountDecimal)
        }
    }

    fun onClickMax() {
        service.balance?.let { balance ->
            fiatServiceNew.setCoinAmount(balance)
        }
    }

    fun onSwitch() {
        switchServiceNew.toggle()
    }

    companion object {
        const val maxValidDecimals = 8
    }

}
