package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class DataProviderSettingsPresenterTest {

    private val provider = mock(FullTransactionInfoModule.Provider::class.java)
    private val interactor = mock(DataProviderSettingsModule.Interactor::class.java)
    private val view = mock(DataProviderSettingsModule.View::class.java)
    private val coinCode = "BTC"
    private val transactionHash = "abc"

    private lateinit var presenter: DataProviderSettingsPresenter

    @Before
    fun setup() {
        whenever(provider.name).thenReturn("abc.com")
        whenever(interactor.baseProvider(coinCode)).thenReturn(provider)
        whenever(interactor.providers(coinCode)).thenReturn(listOf(provider))

        presenter = DataProviderSettingsPresenter(coinCode, transactionHash, interactor)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).baseProvider(coinCode)
        verify(interactor).providers(coinCode)

        val items = listOf(DataProviderSettingsItem(name = "abc.com", online = false, selected = true, checking = true))
        verify(view).show(items)
    }

    @Test
    fun onSelect() {
        val item = DataProviderSettingsItem(name = "abc.com", online = false, selected = false, checking = true)

        presenter.onSelect(item)

        verify(interactor).setBaseProvider(item.name, coinCode)
    }

    @Test
    fun onSetDataProvider() {
        presenter.onSetDataProvider()

        verify(view).close()
    }

    @Test
    fun onPingSuccess() {
        presenter.items = listOf(DataProviderSettingsItem(name = "abc.com", online = false, selected = false, checking = true))
        presenter.onPingSuccess("abc.com")

        verify(view).show(listOf(DataProviderSettingsItem(name = "abc.com", online = true, selected = true, checking = false)))
    }

    @Test
    fun onPingFailure() {
        presenter.items = listOf(DataProviderSettingsItem(name = "abc.com", online = true, selected = false, checking = true))
        presenter.onPingFailure("abc.com")

        verify(view).show(listOf(DataProviderSettingsItem(name = "abc.com", online = false, selected = true, checking = false)))

    }
}
