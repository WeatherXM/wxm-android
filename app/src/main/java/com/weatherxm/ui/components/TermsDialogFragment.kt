package com.weatherxm.ui.components

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherxm.R
import com.weatherxm.databinding.ViewTermsDialogBinding
import com.weatherxm.ui.common.setHtml
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class TermsDialogFragment() : DialogFragment() {
    private lateinit var binding: ViewTermsDialogBinding

    private var onLinkClicked: ((String) -> Unit)? = null
    private var onClick: (() -> Unit)? = null

    companion object {
        const val TAG = "TERMS_DIALOG_FRAGMENT"
        const val AUTO_DISMISS_DIALOG = "AUTO_DISMISS_DIALOG"
    }

    constructor(onLinkClicked: (String) -> Unit, onClick: () -> Unit) : this() {
        this.onLinkClicked = onLinkClicked
        this.onClick = onClick
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        binding = ViewTermsDialogBinding.inflate(layoutInflater)
        builder.setView(binding.root)
        builder.setBackgroundInsetStart(0)
        builder.setBackgroundInsetEnd(0)

        with(binding.message) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    onLinkClicked?.invoke(url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(
                R.string.terms_dialog_message,
                getString(R.string.terms_of_use_owners_url),
                getString(R.string.privacy_policy_owners_url)
            )
        }

        binding.understandBtn.setOnClickListener {
            onClick?.invoke()
            dismiss()
        }

        isCancelable = false

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
        if (onLinkClicked != null && onClick != null) {
            show(activity.supportFragmentManager, TAG)
        }
    }
}
