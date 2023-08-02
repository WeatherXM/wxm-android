package com.weatherxm.ui.connectwallet

import android.app.Activity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityConnectWalletBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.AlertDialogFragment
import com.weatherxm.ui.common.getRichText
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Analytics
import com.weatherxm.util.Mask
import com.weatherxm.util.Validator
import com.weatherxm.util.applyInsets
import com.weatherxm.util.onTextChanged
import com.weatherxm.util.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class ConnectWalletActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityConnectWalletBinding
    private val model: ConnectWalletViewModel by viewModels()
    private val validator: Validator by inject()
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private var snackbar: Snackbar? = null

    // Register the launcher and result handler for QR code scanner
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents.let {
                model.onScanAddress(it)?.let { address ->
                    binding.address.setText(address)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        setListeners()

        binding.walletCompatibilityCard
            .action(
                getString(R.string.check_wallet_compatibility),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_open_new, this.theme)
            ) {
                navigator.openWebsite(this, getString(R.string.suggested_wallets_documentation))
                analytics.trackEventPrompt(
                    Analytics.ParamValue.WALLET_COMPATIBILITY.paramValue,
                    Analytics.ParamValue.INFO.paramValue,
                    Analytics.ParamValue.ACTION.paramValue
                )
            }

        binding.walletCompatibilityCard.closeButton {
            binding.walletCompatibilityCard.setVisible(false)
            analytics.trackEventPrompt(
                Analytics.ParamValue.WALLET_COMPATIBILITY.paramValue,
                Analytics.ParamValue.INFO.paramValue,
                Analytics.ParamValue.DISMISS.paramValue
            )
        }

        with(binding.termsCheckboxDesc) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    analytics.trackEventSelectContent(Analytics.ParamValue.WALLET_TERMS.paramValue)
                    navigator.openWebsite(this@ConnectWalletActivity, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(R.string.accept_terms, getString(R.string.terms_of_service_url))
        }

        // Listen to current address for UI update
        model.currentAddress().observe(this) {
            onAddressUpdateUI(it)
        }

        // Listen for newly saved address state change
        model.isAddressSaved().observe(this) { result ->
            onNewAddressSaved(result)
        }

        binding.address.onTextChanged {
            binding.addressContainer.error = null
        }
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.scanQR.setOnClickListener {
            analytics.trackEventSelectContent(Analytics.ParamValue.WALLET_SCAN_QR.paramValue)
            navigator.showQRScanner(barcodeLauncher)
        }

        binding.editWallet.setOnClickListener {
            analytics.trackEventSelectContent(
                Analytics.ParamValue.EDIT_WALLET.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, model.currentAddress().value ?: "")
            )
            navigator.showPasswordPrompt(
                this,
                R.string.edit_wallet_password_prompt_desc
            ) { isPasswordConfirmed ->
                if (isPasswordConfirmed) {
                    Timber.d("Password confirmation success!")
                    onEditClicked()
                } else {
                    Timber.d("Password confirmation prompt was cancelled or failed.")
                }
            }
        }

        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.saveBtn.isEnabled = binding.ownershipCheckbox.isChecked && isChecked
        }

        binding.ownershipCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.saveBtn.isEnabled = binding.termsCheckbox.isChecked && isChecked
        }

        binding.createMetamask.setOnClickListener {
            analytics.trackEventSelectContent(
                Analytics.ParamValue.CREATE_METAMASK.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, model.currentAddress().value ?: "")
            )
            navigator.openWebsite(this, getString(R.string.suggested_wallets_documentation))
        }

        binding.viewTransactionHistoryBtn.setOnClickListener {
            navigator.openWebsite(
                this, getString(R.string.wallet_explorer, model.currentAddress().value)
            )
            analytics.trackEventSelectContent(
                Analytics.ParamValue.WALLET_TRANSACTIONS.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, model.currentAddress().value ?: "")
            )
        }

        binding.saveBtn.setOnClickListener {
            val address = binding.address.text.toString()

            if (!validator.validateEthAddress(address)) {
                binding.addressContainer.error = getString(R.string.warn_validation_invalid_address)
                return@setOnClickListener
            }

            showConfirmWalletDialog(address)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.WALLET,
            ConnectWalletActivity::class.simpleName
        )
    }

    private fun showConfirmWalletDialog(address: String) {
        AlertDialogFragment
            .Builder(
                title = getString(R.string.confirm_wallet_ownership_title),
                message = getRichText(
                    R.string.confirm_wallet_ownership_desc,
                    model.getLastPartOfAddress(address)
                )
            )
            .onPositiveClick(getString(R.string.action_confirm)) {
                model.saveAddress(address)
            }
            .onNegativeClick(getString(R.string.action_cancel)) {
                // Do nothing
            }
            .build()
            .show(this)
    }

    private fun onEditClicked() {
        binding.editWallet.setVisible(false)
        binding.tokenNoticeCard.setVisible(false)
        binding.viewTransactionHistoryBtn.setVisible(false)
        binding.addressContainer.isEnabled = true
        binding.addressContainer.isCounterEnabled = true
        binding.address.setText(model.currentAddress().value)
        binding.termsCheckbox.isChecked = false
        binding.ownershipCheckbox.isChecked = false
        binding.saveBtn.isEnabled = false
        binding.scanQR.setVisible(true)
        binding.checkBoxesAndButtonContainer.setVisible(true)
        binding.ownershipCheckbox.isEnabled = true
        binding.termsCheckbox.isEnabled = true
        binding.walletCompatibilityCard.setVisible(true)

        analytics.trackEventPrompt(
            Analytics.ParamValue.WALLET_COMPATIBILITY.paramValue,
            Analytics.ParamValue.INFO.paramValue,
            Analytics.ParamValue.VIEW.paramValue
        )
    }

    private fun onAddressUpdateUI(address: String?) {
        if (address.isNullOrEmpty()) {
            binding.editWallet.setVisible(false)
            binding.tokenNoticeCard.setVisible(false)
            binding.viewTransactionHistoryBtn.setVisible(false)
            binding.scanQR.setVisible(true)
            binding.checkBoxesAndButtonContainer.setVisible(true)

            analytics.trackEventPrompt(
                Analytics.ParamValue.WALLET_COMPATIBILITY.paramValue,
                Analytics.ParamValue.INFO.paramValue,
                Analytics.ParamValue.VIEW.paramValue
            )
        } else {
            binding.editWallet.setVisible(true)
            binding.address.setText(Mask.maskHash(address))
            binding.address.isEnabled = false
            binding.addressContainer.isCounterEnabled = false
            binding.scanQR.setVisible(false)
            binding.checkBoxesAndButtonContainer.setVisible(false)
            binding.walletCompatibilityCard.setVisible(false)
            binding.tokenNoticeCard.setVisible(true)
            binding.viewTransactionHistoryBtn.setVisible(true)
        }
    }

    private fun onNewAddressSaved(result: Resource<String>) {
        when (result.status) {
            Status.SUCCESS -> {
                Timber.d("Address saved.")
                result.data?.let {
                    showSnackbarMessage(it)
                }
                binding.loading.setVisible(false)
                setResult(Activity.RESULT_OK)
            }
            Status.ERROR -> {
                result.message?.let {
                    showSnackbarMessage(it)
                }
                binding.loading.setVisible(false)
                setInputEnabled(true)
            }
            Status.LOADING -> {
                binding.loading.setVisible(true)
                setInputEnabled(false)
            }
        }
    }

    private fun setInputEnabled(enabled: Boolean) {
        binding.addressContainer.isEnabled = enabled
        binding.termsCheckbox.isEnabled = enabled
        binding.ownershipCheckbox.isEnabled = enabled
        binding.saveBtn.isEnabled = enabled
    }

    private fun showSnackbarMessage(message: String) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
        snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar?.show()
    }
}
