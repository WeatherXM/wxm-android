package com.weatherxm.ui.resetpassword

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityResetPasswordBinding
import com.weatherxm.util.Validator
import com.weatherxm.util.applyInsets
import com.weatherxm.util.onTextChanged
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

class ResetPasswordActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityResetPasswordBinding
    private val validator: Validator by inject()
    private val model: ResetPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        binding.email.onTextChanged {
            binding.emailContainer.error = null
            binding.sendBtn.isEnabled = !binding.email.text.isNullOrEmpty()
        }

        binding.sendBtn.setOnClickListener {
            val email = binding.email.text.toString().trim()

            if (!validator.validateUsername(email)) {
                binding.emailContainer.error = getString(R.string.invalid_email)
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
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_success, false)
                    .title(getString(R.string.success_reset_password_title))
                    .subtitle(getString(R.string.success_reset_password_text))
                binding.form.visibility = View.GONE
                binding.status.visibility = View.VISIBLE
            }
            Status.ERROR -> {
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_error, false)
                    .title(getString(R.string.oops_something_wrong))
                    .subtitle("${result.message}")
                    .action(getString(R.string.action_retry))
                    .listener {
                        binding.form.visibility = View.VISIBLE
                        binding.status.visibility = View.GONE
                    }
            }
            Status.LOADING -> {
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_loading)
                binding.form.visibility = View.GONE
                binding.status.visibility = View.VISIBLE
            }
        }
    }
}
