package com.weatherxm.ui.resetpassword

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityResetPasswordBinding
import com.weatherxm.ui.common.toast
import com.weatherxm.util.Validator
import com.weatherxm.util.onTextChanged
import dev.chrisbanes.insetter.applyInsetter
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class ResetPasswordActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityResetPasswordBinding
    private val validator: Validator by inject()
    private val model: ResetPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyMapInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        binding.email.onTextChanged {
            binding.emailContainer.error = null
            binding.sendBtn.isEnabled = !binding.email.text.isNullOrEmpty()
        }

        binding.sendBtn.setOnClickListener {
            val email = binding.email.text.toString()

            if (validator.validateUsername(email)) {
                binding.emailContainer.error = model.resHelper().getString(R.string.invalid_email)
                return@setOnClickListener
            }

            model.resetPassword(email)
        }

        model.isEmailSent().observe(this) { result ->
            onEmailSentResult(result)
        }
    }

    private fun onEmailSentResult(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                binding.successIcon.visibility = View.VISIBLE
                binding.successTitle.visibility = View.VISIBLE
                binding.successText.visibility = View.VISIBLE
                binding.progress.visibility = View.INVISIBLE
            }
            Status.ERROR -> {
                binding.sendBtn.isEnabled = true
                binding.progress.visibility = View.INVISIBLE
                result.message?.let { toast(it, Toast.LENGTH_LONG) }
            }
            Status.LOADING -> {
                binding.sendBtn.isEnabled = false
                binding.progress.visibility = View.VISIBLE
            }
        }
    }

    private fun applyMapInsets() {
        binding.root.applyInsetter {
            type(statusBars = true) {
                padding(left = false, top = true, right = false, bottom = false)
            }
            type(navigationBars = true) {
                padding(left = false, top = false, right = false, bottom = true)
            }
        }
    }
}
