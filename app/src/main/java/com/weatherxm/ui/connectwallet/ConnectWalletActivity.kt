package com.weatherxm.ui.connectwallet

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.databinding.ActivityConnectWalletBinding
import com.weatherxm.ui.common.ActionForMessageView
import com.weatherxm.ui.common.DataForMessageView
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.SubtitleForMessageView
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.getRichText
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.compose.MessageCardView
import com.weatherxm.util.Mask
import com.weatherxm.util.Validator
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ConnectWalletActivity : BaseActivity() {
    private lateinit var binding: ActivityConnectWalletBinding
    private val model: ConnectWalletViewModel by viewModel()
    private val scanner: GmsBarcodeScanner by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListeners()
        setWalletCompatibilityCard()

        with(binding.termsCheckboxDesc) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    analytics.trackEventSelectContent(
                        AnalyticsService.ParamValue.WALLET_TERMS.paramValue
                    )
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
            analytics.trackEventSelectContent(AnalyticsService.ParamValue.WALLET_SCAN_QR.paramValue)

            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    model.onScanAddress(barcode.rawValue)?.let { address ->
                        binding.address.setText(address)
                    }
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failure when scanning QR of wallet")
                    toast(
                        R.string.error_scan_exception,
                        e.message ?: String.empty(),
                        Toast.LENGTH_LONG
                    )
                }
        }

        binding.editWallet.setOnClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.EDIT_WALLET.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    model.currentAddress().value ?: String.empty()
                )
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
                AnalyticsService.ParamValue.CREATE_METAMASK.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    model.currentAddress().value ?: String.empty()
                )
            )
            navigator.openWebsite(this, getString(R.string.suggested_wallets_documentation))
        }

        binding.viewTransactionHistoryBtn.setOnClickListener {
            navigator.openWebsite(
                this, getString(R.string.wallet_explorer_arbitrum, model.currentAddress().value)
            )
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.WALLET_TRANSACTIONS.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID, model.currentAddress().value ?: String.empty()
                )
            )
        }

        binding.saveBtn.setOnClickListener {
            val address = binding.address.text.toString()

            if (!Validator.validateEthAddress(address)) {
                binding.addressContainer.error = getString(R.string.warn_validation_invalid_address)
                return@setOnClickListener
            }

            showConfirmWalletDialog(address)
        }
    }

    private fun setWalletCompatibilityCard() {
        binding.walletCompatibilityCard.setContent {
            MessageCardView(
                data = DataForMessageView(
                    title = R.string.wallet_compatibility,
                    subtitle = SubtitleForMessageView(message = R.string.wallet_compatibility_desc),
                    drawable = R.drawable.ic_error_hex_filled,
                    useStroke = true,
                    action = ActionForMessageView(
                        label = R.string.check_wallet_compatibility,
                        endIcon = R.drawable.ic_open_new,
                        onClickListener = {
                            navigator.openWebsite(
                                this,
                                getString(R.string.suggested_wallets_documentation)
                            )
                            analytics.trackEventPrompt(
                                AnalyticsService.ParamValue.WALLET_COMPATIBILITY.paramValue,
                                AnalyticsService.ParamValue.INFO.paramValue,
                                AnalyticsService.ParamValue.ACTION.paramValue
                            )
                        }
                    ),
                    severityLevel = SeverityLevel.ERROR,
                    onCloseListener = {
                        binding.walletCompatibilityCard.visible(false)
                        analytics.trackEventPrompt(
                            AnalyticsService.ParamValue.WALLET_COMPATIBILITY.paramValue,
                            AnalyticsService.ParamValue.INFO.paramValue,
                            AnalyticsService.ParamValue.DISMISS.paramValue
                        )
                    }
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.WALLET, classSimpleName())
    }

    private fun showConfirmWalletDialog(address: String) {
        ActionDialogFragment
            .Builder(
                title = getString(R.string.confirm_wallet_ownership_title),
                message = getRichText(
                    R.string.confirm_wallet_ownership_desc,
                    model.getLastPartOfAddress(address)
                ),
                negative = getString(R.string.action_cancel)
            )
            .onPositiveClick(getString(R.string.action_confirm)) {
                model.setWalletAddress(address)
            }
            .build()
            .show(this)
    }

    private fun onEditClicked() {
        binding.editWallet.visible(false)
        binding.viewTransactionHistoryBtn.visible(false)
        binding.addressContainer.isEnabled = true
        binding.addressContainer.isCounterEnabled = true
        binding.address.setText(model.currentAddress().value)
        binding.termsCheckbox.isChecked = false
        binding.ownershipCheckbox.isChecked = false
        binding.saveBtn.isEnabled = false
        binding.scanQR.visible(true)
        binding.checkBoxesAndButtonContainer.visible(true)
        binding.ownershipCheckbox.isEnabled = true
        binding.termsCheckbox.isEnabled = true
        binding.walletCompatibilityCard.visible(true)

        analytics.trackEventPrompt(
            AnalyticsService.ParamValue.WALLET_COMPATIBILITY.paramValue,
            AnalyticsService.ParamValue.INFO.paramValue,
            AnalyticsService.ParamValue.VIEW.paramValue
        )
    }

    private fun onAddressUpdateUI(address: String) {
        if (address.isEmpty()) {
            binding.editWallet.visible(false)
            binding.viewTransactionHistoryBtn.visible(false)
            binding.scanQR.visible(true)
            binding.checkBoxesAndButtonContainer.visible(true)

            analytics.trackEventPrompt(
                AnalyticsService.ParamValue.WALLET_COMPATIBILITY.paramValue,
                AnalyticsService.ParamValue.INFO.paramValue,
                AnalyticsService.ParamValue.VIEW.paramValue
            )
        } else {
            binding.editWallet.visible(true)
            binding.address.setText(Mask.maskHash(address))
            binding.address.isEnabled = false
            binding.addressContainer.isCounterEnabled = false
            binding.scanQR.visible(false)
            binding.checkBoxesAndButtonContainer.visible(false)
            binding.walletCompatibilityCard.visible(false)
            binding.viewTransactionHistoryBtn.visible(true)
        }
    }

    private fun onNewAddressSaved(result: Resource<String>) {
        when (result.status) {
            Status.SUCCESS -> {
                Timber.d("Address saved.")
                result.data?.let {
                    showSnackbarMessage(binding.root, it)
                }
                binding.loading.visible(false)
                setResult(Activity.RESULT_OK)
            }
            Status.ERROR -> {
                result.message?.let {
                    showSnackbarMessage(binding.root, it)
                }
                binding.loading.visible(false)
                setInputEnabled(true)
            }
            Status.LOADING -> {
                binding.loading.visible(true)
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
}
