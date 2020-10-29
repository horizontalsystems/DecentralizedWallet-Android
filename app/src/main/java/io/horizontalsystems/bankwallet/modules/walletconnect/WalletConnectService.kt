package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.WCSessionStoreItem
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WalletConnectService(
        ethKitManager: IEthereumKitManager,
        private val sessionStore: WalletConnectSessionStore,
        private val connectivityManager: ConnectivityManager
) : WalletConnectInteractor.Delegate, Clearable {

    sealed class State {
        object Idle : State()
        object WaitingForApproveSession : State()
        object Ready : State()
        object Killed : State()
    }

    data class PeerData(val peerId: String, val peerMeta: WCPeerMeta)

    private val ethereumKit: EthereumKit? = ethKitManager.ethereumKit
    private var interactor: WalletConnectInteractor? = null
    private var remotePeerData: PeerData? = null
    val remotePeerMeta: WCPeerMeta?
        get() = remotePeerData?.peerMeta

    var state: State = State.Idle
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    val connectionState: WalletConnectInteractor.State
        get() = interactor?.state ?: WalletConnectInteractor.State.Disconnected

    val stateSubject = PublishSubject.create<State>()
    val connectionStateSubject = PublishSubject.create<WalletConnectInteractor.State>()
    val requestSubject = PublishSubject.create<WalletConnectRequest>()

    val isEthereumKitReady: Boolean
        get() = ethereumKit != null

    private val pendingRequests = mutableMapOf<Long, WalletConnectRequest>()
    private val disposable = CompositeDisposable()

    init {
        val sessionStoreItem = sessionStore.storedItem

        if (sessionStoreItem != null) {
            remotePeerData = PeerData(sessionStoreItem.remotePeerId, sessionStoreItem.remotePeerMeta)

            interactor = WalletConnectInteractor(sessionStoreItem.session, sessionStoreItem.peerId)
            interactor?.delegate = this
            interactor?.connect(sessionStoreItem.remotePeerId)

            state = State.Ready
        } else {
            state = State.Idle
        }

        connectivityManager.networkAvailabilitySignal
                .subscribe {
                    if (connectivityManager.isConnected) {
                        remotePeerData?.peerId?.let { remotePeerId ->
                            interactor?.connect(remotePeerId)
                        }
                    }
                }
                .let {
                    disposable.add(it)
                }
    }


    fun connect(uri: String) {
        interactor = WalletConnectInteractor(uri)
        interactor?.delegate = this
        interactor?.connect(null)
    }

    override fun clear() {
        disposable.clear()
        interactor?.disconnect()
    }

    fun approveSession() {
        ethereumKit?.let { ethereumKit ->
            interactor?.let { interactor ->
                val chainId = ethereumKit.networkType.getNetwork().id
                interactor.approveSession(ethereumKit.receiveAddress.eip55, chainId)

                remotePeerData?.let { peerData ->
                    sessionStore.storedItem = WCSessionStoreItem(interactor.session, chainId, interactor.peerId, peerData.peerId, peerData.peerMeta)
                }

                state = State.Ready
            }
        }
    }

    fun rejectSession() {
        interactor?.let {
            it.rejectSession()

            state = State.Killed
        }
    }

    fun killSession() {
        interactor?.killSession()
    }

    fun approveRequest(requestId: Long) {
        TODO("Not yet implemented")
    }

    fun rejectRequest(requestId: Long) {
        pendingRequests.remove(requestId)

        interactor?.rejectRequest(requestId, "Rejected by user")
    }

    override fun didUpdateState(state: WalletConnectInteractor.State) {
        connectionStateSubject.onNext(state)
    }

    override fun didKillSession() {
        sessionStore.storedItem = null

        state = State.Killed
    }

    override fun didRequestSession(remotePeerId: String, remotePeerMeta: WCPeerMeta) {
        this.remotePeerData = PeerData(remotePeerId, remotePeerMeta)

        state = State.WaitingForApproveSession
    }

    override fun didRequestSendEthTransaction(id: Long, transaction: WCEthereumTransaction) {
        handleRequest(id) {
            WalletConnectSendEthereumTransactionRequest(id, transaction)
        }
    }

    private fun handleRequest(id: Long, requestResolver: () -> WalletConnectRequest) {
        try {
            val request = requestResolver()

            pendingRequests[request.id] = request
            requestSubject.onNext(request)
        } catch (t: Throwable) {
            interactor?.rejectRequest(id, t.message ?: "")
        }
    }
}
