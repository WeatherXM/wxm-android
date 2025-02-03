package com.weatherxm.ui.components

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherxm.databinding.ViewLoginPromptDialogBinding
import com.weatherxm.ui.common.setHtml

class LoginPromptDialogFragment() : DialogFragment() {
    private lateinit var binding: ViewLoginPromptDialogBinding

    private var title: String? = null
    private var message: String? = null
    private var htmlMessage: String? = null
    private var onLogin: (() -> Unit)? = null
    private var onSignup: (() -> Unit)? = null

    companion object {
        const val TAG = "LOGIN_PROMPT_DIALOG_FRAGMENT"
        const val AUTO_DISMISS_DIALOG = "AUTO_DISMISS_DIALOG"
    }

    constructor(
        title: String,
        message: String? = null,
        htmlMessage: String? = null,
        onLogin: () -> Unit,
        onSignup: () -> Unit
    ) : this() {
        this.title = title
        this.message = message
        this.htmlMessage = htmlMessage
        this.onLogin = onLogin
        this.onSignup = onSignup
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        binding = ViewLoginPromptDialogBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.title.text = title

        message?.let {
            binding.message.text = it
        }

        htmlMessage?.let {
            binding.message.setHtml(it)
        }

        binding.closePopup.setOnClickListener {
            dismiss()
        }

        binding.loginBtn.setOnClickListener {
            onLogin?.invoke()
            dismiss()
        }

        binding.signupBtn.setOnClickListener {
            onSignup?.invoke()
            dismiss()
        }

        /**
         * Enabling "Don't keep activities" causes a crash, and we cannot save the listeners
         * in the savedInstanceState to restore the dialog so we dismiss it automatically
         * and it's the caller's responsibility to re-instantiate it.
         */
        savedInstanceState?.let {
            if (it.getBoolean(AUTO_DISMISS_DIALOG, false)) {
                dismiss()
            }
        }

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(AUTO_DISMISS_DIALOG, true)
        super.onSaveInstanceState(outState)
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager, TAG)
    }
}
