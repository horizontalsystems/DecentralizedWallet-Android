package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.modules.market.top.Field
import io.horizontalsystems.bankwallet.modules.market.top.IMarketListDataSource
import io.horizontalsystems.xrateskit.entities.Coin
import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.reactivex.Observable
import io.reactivex.Single

class MarketListFavoritesDataSource(
        private val xRateManager: IRateManager,
        private val marketFavoritesManager: MarketFavoritesManager
) : IMarketListDataSource {

    override val sortingFields: Array<Field> = Field.values()
    override val dataUpdatedAsync: Observable<Unit> by marketFavoritesManager::dataUpdatedAsync

    override fun getListAsync(currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<CoinMarket>> {
        val coins = marketFavoritesManager.getAll().map { favoriteCoin ->
            Coin(favoriteCoin.code, type = favoriteCoin.coinType?.let { xRateManager.convertCoinTypeToXRateKitCoinType(it) })
        }

        return xRateManager.getCoinMarketList(coins, currencyCode, fetchDiffPeriod)
    }

}
