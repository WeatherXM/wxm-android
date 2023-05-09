package com.weatherxm.ui.connectwallet

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
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

        binding.walletCompatibilityCard
            .action(
                getString(R.string.check_wallet_compatibility),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_open_new, this.theme)
            ) {
                navigator.openWebsite(this, getString(R.string.suggested_wallets_documentation))
            }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.scanQR.setOnClickListener {
            navigator.showQRScanner(barcodeLauncher)
        }

        binding.editWallet.setOnClickListener {
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

        binding.address.onTextChanged {
            binding.addressContainer.error = null
        }

        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.saveBtn.isEnabled = binding.ownershipCheckbox.isChecked && isChecked
        }

        binding.ownershipCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.saveBtn.isEnabled = binding.termsCheckbox.isChecked && isChecked
        }

        with(binding.termsCheckboxDesc) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    navigator.openWebsite(this@ConnectWalletActivity, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(R.string.accept_terms, getString(R.string.terms_of_service_url))
        }

        binding.createMetamask.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.suggested_wallets_documentation))
        }

        // Listen to current address for UI update
        model.currentAddress().observe(this) {
            onAddressUpdateUI(it)
        }

        binding.saveBtn.setOnClickListener {
            val address = binding.address.text.toString()

            if (!validator.validateEthAddress(address)) {
                binding.addressContainer.error = getString(R.string.warn_validation_invalid_address)
                return@setOnClickListener
            }

            showConfirmWalletDialog(address)
        }

        // Listen for newly saved address state change
        model.isAddressSaved().observe(this) { result ->
            onNewAddressSaved(result)
        }

        onBackPressedDispatcher.addCallback {
            finish()
        }
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
        binding.editWallet.visibility = View.GONE
        binding.addressContainer.isEnabled = true
        binding.addressContainer.isCounterEnabled = true
        binding.address.setText(model.currentAddress().value)
        binding.termsCheckbox.isChecked = false
        binding.ownershipCheckbox.isChecked = false
        binding.saveBtn.isEnabled = false
        binding.scanQR.visibility = View.VISIBLE
        binding.checkBoxesAndButtonContainer.visibility = View.VISIBLE
        binding.ownershipCheckbox.isEnabled = true
        binding.termsCheckbox.isEnabled = true
        binding.walletCompatibilityCard.setVisible(true)
    }

    private fun onAddressUpdateUI(address: String?) {
        if (address.isNullOrEmpty()) {
            binding.editWallet.visibility = View.GONE
            binding.scanQR.visibility = View.VISIBLE
            binding.checkBoxesAndButtonContainer.visibility = View.VISIBLE
        } else {
            binding.editWallet.visibility = View.VISIBLE
            binding.address.setText(Mask.maskHash(address))
            binding.address.isEnabled = false
            binding.addressContainer.isCounterEnabled = false
            binding.scanQR.visibility = View.GONE
            binding.checkBoxesAndButtonContainer.visibility = View.GONE
            binding.walletCompatibilityCard.setVisible(false)
        }
    }

    private fun onNewAddressSaved(result: Resource<String>) {
        when (result.status) {
            Status.SUCCESS -> {
                Timber.d("Address saved.")
                result.data?.let {
                    showSnackbarMessage(it)
                }
                binding.loading.visibility = View.GONE
                setResult(Activity.RESULT_OK)
            }
            Status.ERROR -> {
                result.message?.let {
                    showSnackbarMessage(it)
                }
                binding.loading.visibility = View.GONE
                setInputEnabled(true)
            }
            Status.LOADING -> {
                binding.loading.visibility = View.VISIBLE
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
