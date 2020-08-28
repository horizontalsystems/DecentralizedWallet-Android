package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.security.KeyStoreValidationResult

class KeyStoreChangeListener(private val systemInfoManager: ISystemInfoManager, private val keyStoreManager: IKeyStoreManager)
    : BackgroundManager.Listener {

    override fun willEnterForeground(activity: Activity) {
        if (systemInfoManager.isSystemLockOff){
            KeyStoreActivity.startForNoSystemLock(activity)
        }

        when(keyStoreManager.validateKeyStore()) {
            KeyStoreValidationResult.UserNotAuthenticated -> KeyStoreActivity.startForUserAuthentication(activity)
            KeyStoreValidationResult.KeyIsInvalid -> KeyStoreActivity.startForInvalidKey(activity)
            KeyStoreValidationResult.KeyIsValid -> { /* Do nothing */}
        }
    }
}
