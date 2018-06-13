package org.grouvi.wallet.modules.addWallet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_wallet.*
import org.grouvi.wallet.R
import org.grouvi.wallet.modules.backupWords.BackupWordsModule
import org.grouvi.wallet.modules.restoreWallet.RestoreWalletModule

class AddWalletActivity : AppCompatActivity() {

    private lateinit var viewModel: AddWalletViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_wallet)

        viewModel = ViewModelProviders.of(this).get(AddWalletViewModel::class.java)
        viewModel.init()

        viewModel.openBackupScreenLiveEvent.observe(this, Observer {
            BackupWordsModule.start(this, BackupWordsModule.DismissMode.TO_MAIN)
            finish()
        })

        viewModel.openRestoreWalletScreenLiveEvent.observe(this, Observer {
            RestoreWalletModule.start(this)
        })

        buttonCreate.setOnClickListener {
            viewModel.presenter.createWallet()
        }

        buttonRestore.setOnClickListener {
            viewModel.presenter.restoreWallet()
        }
    }
}
