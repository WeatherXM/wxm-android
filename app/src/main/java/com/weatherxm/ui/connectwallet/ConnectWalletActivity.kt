package com.weatherxm.ui.connectwallet

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.data.Wallet
import com.weatherxm.databinding.ActivityConnectWalletBinding
import com.weatherxm.ui.common.toast
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.applyTopBottomInsets
import com.weatherxm.util.onTextChanged
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.util.*

class ConnectWalletActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityConnectWalletBinding
    private val model: ConnectWalletViewModel by viewModels()
    private val resHelper: ResourcesHelper by inject()
    private var snackbar: Snackbar? = null

    // Register the launcher and result handler for QR code scanner
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents.let { address ->
                binding.address.setText(address)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyTopBottomInsets()

        val wallet = intent?.extras?.getParcelable<Wallet>(ARG_WALLET)
        wallet?.address?.let {
            showCurrentAddress(it)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.addressContainer.setEndIconOnClickListener {
            barcodeLauncher.launch(ScanOptions().setBeepEnabled(false))
        }

        binding.address.onTextChanged {
            binding.addressContainer.error = null
            binding.saveBtn.isEnabled = !binding.address.text.isNullOrEmpty()
        }

        binding.saveBtn.setOnClickListener {
            if (!model.isAddressValid(binding.address.text.toString())) {
                binding.addressContainer.error = getString(R.string.invalid_address)
                return@setOnClickListener
            }

            if (!binding.termsCheckbox.isChecked || !binding.ownershipCheckbox.isChecked) {
                toast(R.string.checkbox_not_checked, Toast.LENGTH_LONG)
                return@setOnClickListener
            }

            binding.loading.visibility = View.VISIBLE
            binding.saveBtn.isEnabled = false
            model.saveAddress(binding.address.text.toString())
        }

        // Listen for login state change
        model.isAddressSaved().observe(this) { result ->
            onAddressSaved(result)
        }
    }

    private fun showCurrentAddress(address: String) {
        binding.toolbar.title = resHelper.getString(R.string.title_change_wallet)
        binding.currentAddressTitle.visibility = View.VISIBLE
        binding.currentAddress.visibility = View.VISIBLE
        binding.currentAddress.text = address
    }

    private fun onAddressSaved(result: Resource<String>) {
        when (result.status) {
            Status.SUCCESS -> {
                updateUI(true)
                Timber.d("Address saved.")
                result.data?.let {
                    showSnackbarMessage(it)
                }
                showCurrentAddress(binding.address.text.toString())
            }
            Status.ERROR -> {
                updateUI(true)
                binding.loading.visibility = View.INVISIBLE
                result.message?.let {
                    showSnackbarMessage(it)
                }
            }
            Status.LOADING -> {
                updateUI(false)
                binding.loading.visibility = View.VISIBLE
            }
        }
    }

    private fun updateUI(buttonAddressEnabled: Boolean) {
        binding.saveBtn.isEnabled = buttonAddressEnabled
        binding.address.isEnabled = buttonAddressEnabled
    }

    private fun showSnackbarMessage(message: String) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
        snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar?.show()
    }

    companion object {
        const val ARG_WALLET = "wallet"
    }
}
