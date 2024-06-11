package com.weatherxm.ui.claimdevice.wifi.manualdetails

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import coil.ImageLoader
import com.redmadrobot.inputmask.MaskedTextChangedListener
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimWifiManualDetailsBinding
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.loadImage
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.unmask
import com.weatherxm.ui.components.BaseFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ClaimWifiManualDetailsFragment : BaseFragment() {
    private val model: ClaimWifiViewModel by activityViewModel()
    private val imageLoader: ImageLoader by inject()
    private lateinit var binding: FragmentClaimWifiManualDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimWifiManualDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Convert typed characters to uppercase
        binding.serialNumber.filters = arrayOf(InputFilter.AllCaps())

        // Mask serial number input
        MaskedTextChangedListener.installOn(
            editText = binding.serialNumber,
            primaryFormat = if (model.deviceType == DeviceType.M5_WIFI) {
                M5_SERIAL_NUMBER_MASK
            } else {
                D1_SERIAL_NUMBER_MASK
            },
            autocomplete = false,
            autoskip = true,
            valueListener = object : MaskedTextChangedListener.ValueListener {
                override fun onTextChanged(
                    maskFilled: Boolean,
                    extractedValue: String,
                    formattedValue: String,
                    tailPlaceholder: String
                ) {
                    val requiredLength = if (model.deviceType == DeviceType.M5_WIFI) {
                        M5_SERIAL_MAX_LENGTH
                    } else {
                        D1_SERIAL_MAX_LENGTH
                    }
                    binding.serialNumberContainer.error = null
                    binding.serialNumberContainer.helperText =
                        "${extractedValue.length}/$requiredLength"
                    handleProceedBtnStatus(extractedValue, binding.claimingKey.text.toString())
                }
            })

        binding.claimingKey.onTextChanged {
            binding.claimingKeyContainer.error = null
            binding.claimingKeyContainer.helperText = "${it.length}/$CLAIMING_KEY_MAX_LENGTH"
            handleProceedBtnStatus(binding.serialNumber.text.unmask(), it)
        }

        binding.claimingKey.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.serialNumber.requestFocus()
            }
            true
        }

        binding.serialNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateAndSetDetails()
            }
            true
        }

        binding.proceedBtn.setOnClickListener {
            validateAndSetDetails()
        }

        if (model.deviceType == DeviceType.M5_WIFI) {
            binding.title.text = getString(R.string.enter_gateway_serial_number)
            binding.desc.setHtml(R.string.enter_gateway_serial_number_desc)
            binding.guideImage.loadImage(imageLoader, R.raw.m5_claim)
        } else {
            binding.title.text = getString(R.string.enter_gateway_details)
            binding.desc.setHtml(R.string.enter_gateway_details_desc)
            binding.guideImage.setImageResource(R.drawable.d1_qr_claim)
        }
        binding.claimingKeyTitle.setVisible(model.deviceType == DeviceType.D1_WIFI)
        binding.claimingKeyContainer.setVisible(model.deviceType == DeviceType.D1_WIFI)
        binding.m5Notice.setVisible(model.deviceType == DeviceType.M5_WIFI)
    }

    private fun handleProceedBtnStatus(serial: String, claimingKey: String?) {
        binding.proceedBtn.isEnabled = if (model.deviceType == DeviceType.M5_WIFI) {
            serial.length == M5_SERIAL_MAX_LENGTH
        } else {
            serial.length == D1_SERIAL_MAX_LENGTH && claimingKey?.length == CLAIMING_KEY_MAX_LENGTH
        }
    }

    private fun validateAndSetDetails() {
        val isSerialValid = model.validateSerial(binding.serialNumber.text.toString())
        val isKeyValid = model.validateClaimingKey(binding.claimingKey.text.toString())

        if (!isSerialValid) {
            binding.serialNumberContainer.error = getString(R.string.warn_validation_invalid_serial)
        } else if (model.deviceType == DeviceType.D1_WIFI && !isKeyValid) {
            binding.claimingKeyContainer.error =
                getString(R.string.warn_validation_invalid_claiming_key)
        } else {
            model.setSerialNumber(binding.serialNumber.text.toString())
            if (model.deviceType == DeviceType.D1_WIFI) {
                model.setClaimingKey(binding.claimingKey.text.toString())
            }
            model.next()
        }
    }

    companion object {
        const val M5_SERIAL_NUMBER_MASK = "[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]"
        const val D1_SERIAL_NUMBER_MASK = "[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]:[__]"
        const val M5_SERIAL_MAX_LENGTH = 18
        const val D1_SERIAL_MAX_LENGTH = 20
        const val CLAIMING_KEY_MAX_LENGTH = 6
    }
}
