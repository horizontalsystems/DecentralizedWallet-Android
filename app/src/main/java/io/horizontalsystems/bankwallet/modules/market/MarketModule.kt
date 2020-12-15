package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MarketModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketCategoriesService(App.marketStorage)
            return MarketCategoriesViewModel(service) as T
        }

    }

}
