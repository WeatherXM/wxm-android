package com.weatherxm.ui.components

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherxm.databinding.ViewLoginPromptDialogBinding
import com.weatherxm.util.setHtml
import org.koin.core.component.KoinComponent

class LoginPromptDialogFragment(
    private val title: String?,
    private val message: String?,
    private val htmlMessage: String?,
    private val onLogin: () -> Unit,
    private val onSignup: () -> Unit
) : DialogFragment(), KoinComponent {
    private lateinit var binding: ViewLoginPromptDialogBinding

    companion object {
        const val TAG = "LOGIN_PROMPT_DIALOG_FRAGMENT"
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
            onLogin.invoke()
            dismiss()
        }

        binding.signupBtn.setOnClickListener {
            onSignup.invoke()
            dismiss()
        }

        return builder.create()
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager, TAG)
    }
}
