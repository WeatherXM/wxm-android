package com.weatherxm.ui.connectwallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityConnectWalletBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.Mask
import com.weatherxm.util.Validator
import com.weatherxm.util.applyInsets
import com.weatherxm.util.clear
import com.weatherxm.util.onTextChanged
import com.weatherxm.util.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class ConnectWalletActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityConnectWalletBinding
    private val model: ConnectWalletViewModel by viewModels()
    private val mask: Mask by inject()
    private val validator: Validator by inject()
    private val navigator: Navigator by inject()
    private var snackbar: Snackbar? = null
    private var onBackGoHome = false

    // Register the launcher and result handler for QR code scanner
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents.let {
                model.onScanAddress(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        onBackGoHome = intent?.extras?.getBoolean(ARG_ON_BACK_GO_HOME) ?: false

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.newAddressContainer.setEndIconOnClickListener {
            scanWallet()
        }

        binding.newAddress.onTextChanged {
            binding.newAddressContainer.error = null
            binding.saveBtn.isEnabled = !binding.newAddress.text.isNullOrEmpty()
        }

        model.newAddress().observe(this) { address ->
            address?.let {
                binding.newAddress.setText(it)
            }
        }

        with(binding.termsCheckbox) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    navigator.openWebsite(this@ConnectWalletActivity, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(R.string.accept_terms, getString(R.string.terms_of_service_url))
        }

        binding.openDocumentation.setOnClickListener {
            navigator.openWebsite(this, getString(R.string.suggested_wallets_documentation))
        }

        // Listen to current address for UI update
        model.currentAddress().observe(this) { address ->
            if (address.isNullOrEmpty()) {
                binding.warning.visibility = View.VISIBLE
                binding.currentAddressContainer.visibility = View.GONE
                binding.currentAddressTitle.visibility = View.GONE
                binding.newAddress.setText("")
            } else {
                binding.warning.visibility = View.GONE
                binding.currentAddressContainer.visibility = View.VISIBLE
                binding.currentAddressTitle.visibility = View.VISIBLE
                binding.currentAddress.setText(mask.maskHash(address))
                binding.currentAddressContainer.setEndIconOnClickListener {
                    shareAddress(address)
                }
            }
        }

        // Changing the newAddress field should update the form's state in the view model
        // and then the form as a whole (address + checkboxes) should be validated there
        binding.saveBtn.setOnClickListener {
            val address = binding.newAddress.text.toString()

            if (!validator.validateEthAddress(address)) {
                binding.newAddressContainer.error =
                    getString(R.string.warn_validation_invalid_address)
                return@setOnClickListener
            }

            model.saveAddress(
                address,
                binding.termsCheckbox.isChecked,
                binding.ownershipCheckbox.isChecked
            )
        }

        // Listen for newly saved address state change
        model.isAddressSaved().observe(this) { result ->
            onAddressSaved(result)
        }

        onBackPressedDispatcher.addCallback {
            if (onBackGoHome) {
                navigator.showHome(this@ConnectWalletActivity)
                finish()
            } else {
                finish()
            }
        }
    }

    private fun scanWallet() {
        barcodeLauncher.launch(
            ScanOptions().setBeepEnabled(false)
        )
    }

    private fun shareAddress(address: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, address)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.title_share_wallet)))
    }

    private fun onAddressSaved(result: Resource<String>) {
        when (result.status) {
            Status.SUCCESS -> {
                Timber.d("Address saved.")
                binding.newAddress.clear()
                result.data?.let {
                    showSnackbarMessage(it)
                }
                setProgressEnabled(false)
                setInputEnabled(true)
                setResult(Activity.RESULT_OK)
            }
            Status.ERROR -> {
                result.message?.let {
                    showSnackbarMessage(it)
                }
                setProgressEnabled(false)
                setInputEnabled(true)
            }
            Status.LOADING -> {
                setProgressEnabled(true)
                setInputEnabled(false)
            }
        }
    }

    private fun setProgressEnabled(enabled: Boolean) {
        binding.loading.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    private fun setInputEnabled(enabled: Boolean) {
        binding.newAddressContainer.isEnabled = enabled
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

    companion object {
        const val ARG_ON_BACK_GO_HOME = "on_back_go_home"
    }
}
