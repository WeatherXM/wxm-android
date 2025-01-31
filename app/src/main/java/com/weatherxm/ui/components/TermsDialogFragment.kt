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

class TermsDialogFragment(
    private val onLinkClicked: (String) -> Unit,
    private val onClick: () -> Unit
) : DialogFragment() {
    private lateinit var binding: ViewTermsDialogBinding

    companion object {
        const val TAG = "TERMS_DIALOG_FRAGMENT"
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
                    onLinkClicked(url)
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
            onClick.invoke()
            dismiss()
        }

        isCancelable = false

        return builder.create()
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager, TAG)
    }
}
