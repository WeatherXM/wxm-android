package com.weatherxm.ui.deleteaccount

import android.app.Activity
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityDeleteAccountBinding
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeleteAccountActivity : BaseActivity() {
    private lateinit var binding: ActivityDeleteAccountBinding
    private val model: DeleteAccountViewModel by viewModel()

    // Register the launcher for the claim device activity and wait for a possible result
    private val surveyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                toast(getString(R.string.thank_you_feedback))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initHtmlText()
        initListeners()

        model.onStatus().observe(this) {
            if (it.data?.status == com.weatherxm.ui.deleteaccount.Status.ACCOUNT_DELETION) {
                onDeletingAccount(it)
            } else {
                onPasswordVerification(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DELETE_ACCOUNT, classSimpleName())
    }

    private fun initHtmlText() {
        with(binding) {
            quickDescription.setHtml(R.string.deletion_account_quick_description)
            deletedData.setHtml(R.string.deletion_account_what_is_deleted)
            notDeletedData.setHtml(R.string.deletion_account_what_is_not_deleted)
            doNotCollectData.setHtml(R.string.deletion_account_do_not_collect_data)
            with(askSupport) {
                movementMethod = BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, _ ->
                        navigator.openSupportCenter(context)
                        return@setOnLinkClickListener true
                    }
                    setHtml(
                        R.string.deletion_account_ask_support,
                        getString(R.string.support_center_url)
                    )
                }
            }
        }
    }

    private fun initListeners() {
        with(binding) {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
                finish()
            }

            password.onTextChanged {
                passwordContainer.error = null
                deleteAccount.isEnabled = !password.text.isNullOrEmpty() && switcher.isChecked
            }

            switcher.setOnCheckedChangeListener { _, checked ->
                if (!password.text.isNullOrEmpty()) deleteAccount.isEnabled = checked
            }

            deleteAccount.setOnClickListener {
                model.checkAndDeleteAccount(password.text.toString().trim())
                deleteAccount.isEnabled = false
            }

            cancelDeletion.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
                finish()
            }

            finishBtn.setOnClickListener {
                navigator.showStartup(this@DeleteAccountActivity)
                finish()
            }

            takeSurvey.setOnClickListener {
                navigator.showDeleteAccountSurvey(surveyLauncher, this@DeleteAccountActivity)
            }

            retryDeletion.setOnClickListener {
                model.checkAndDeleteAccount(password.text.toString().trim())
            }
        }

        onBackPressedDispatcher.addCallback(this, false) {
            if (model.isOnSafeState()) {
                if (model.isAccountedDeleted()) {
                    navigator.showStartup(this@DeleteAccountActivity)
                }
                finish()
            }
        }
    }

    private fun onPasswordVerification(resource: Resource<State>) {
        when (resource.status) {
            Status.ERROR -> {
                binding.statusCard.visible(false)
                binding.infoContainer.visible(true)
                binding.checkboxCard.visible(true)
                if (resource.data?.failure is InvalidPassword) {
                    binding.passwordContainer.error = resource.message
                    binding.passwordContainer.isErrorEnabled = true
                } else {
                    binding.deleteAccount.isEnabled = true
                    resource.message?.let { toast(it) }
                }
            }
            Status.LOADING -> {
                binding.infoContainer.visible(false)
                binding.checkboxCard.visible(false)
                binding.successButtons.visible(false)
                binding.failureButtons.visible(false)
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.empty.title(R.string.validating_password)
                binding.empty.subtitle(null)
                binding.statusCard.visible(true)
            }
            else -> {
                toast(R.string.error_reach_out)
            }
        }
    }

    private fun onDeletingAccount(result: Resource<State>) {
        with(binding) {
            when (result.status) {
                Status.SUCCESS -> {
                    empty.clear()
                        .animation(R.raw.anim_trash_success, false)
                        .title(R.string.deletion_success_title)
                        .subtitle(R.string.deletion_success_message)
                    successButtons.visible(true)
                    failureButtons.visible(false)
                }
                Status.ERROR -> {
                    empty.clear()
                        .animation(R.raw.anim_trash_error, false)
                        .title(R.string.deletion_failure_title)
                        .htmlSubtitle(R.string.deletion_failure_message, result.message) {
                            navigator.openSupportCenter(this@DeleteAccountActivity)
                        }
                        .action(getString(R.string.contact_support_title))
                        .listener {
                            navigator.openSupportCenter(this@DeleteAccountActivity)
                        }
                    successButtons.visible(false)
                    failureButtons.visible(true)
                }
                Status.LOADING -> {
                    empty.clear()
                        .animation(R.raw.anim_trash_loading)
                        .title(R.string.deleting_account)
                }
            }
        }
    }
}
