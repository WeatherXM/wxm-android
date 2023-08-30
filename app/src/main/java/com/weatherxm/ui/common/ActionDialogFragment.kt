package com.weatherxm.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherxm.databinding.ViewActionDialogBinding
import com.weatherxm.util.setHtml
import timber.log.Timber

class ActionDialogFragment : DialogFragment() {

    fun interface OnDialogActionClickListener {
        fun onClick()
    }

    @Suppress("LongParameterList")
    class Builder(
        var title: String? = null,
        var message: String? = null,
        var htmlMessage: String? = null,
        var positive: String? = null,
        var negative: String? = null,
        var onPositiveClickListener: OnDialogActionClickListener? = null,
        var onNegativeClickListener: OnDialogActionClickListener? = null
    ) {
        fun title(title: String) = apply {
            this.title = title
        }

        fun message(message: String) = apply {
            this.message = message
        }

        fun onPositiveClick(label: String, listener: OnDialogActionClickListener): Builder = apply {
            this.positive = label
            this.onPositiveClickListener = listener
        }

        fun onNegativeClick(label: String, listener: OnDialogActionClickListener): Builder = apply {
            this.negative = label
            this.onNegativeClickListener = listener
        }

        fun build(): ActionDialogFragment {
            Timber.d("Building ActionDialogFragment with params: $this")
            return ActionDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to this@Builder.title,
                    ARG_MESSAGE to this@Builder.message,
                    ARG_HTML_MESSAGE to this@Builder.htmlMessage,
                    ARG_POSITIVE to this@Builder.positive,
                    ARG_NEGATIVE to this@Builder.negative
                )
                onPositiveClickListener = this@Builder.onPositiveClickListener
                onNegativeClickListener = this@Builder.onNegativeClickListener
            }
        }
    }

    companion object {
        const val TAG = "ActionDialogFragment"

        private const val REQUEST_KEY = "error_dialog_request_key"
        private const val KEY_RESULT = "result"

        private const val RESULT_POSITIVE = "positive"
        private const val RESULT_NEGATIVE = "negative"

        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_HTML_MESSAGE = "html_message"
        private const val ARG_POSITIVE = "positive"
        private const val ARG_NEGATIVE = "negative"
    }

    private var onPositiveClickListener: OnDialogActionClickListener? = null
    private var onNegativeClickListener: OnDialogActionClickListener? = null
    private lateinit var binding: ViewActionDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        binding = ViewActionDialogBinding.inflate(layoutInflater)

        builder.setView(binding.root)

        arguments?.getString(ARG_TITLE)?.let {
            binding.title.text = it
        }

        arguments?.getString(ARG_MESSAGE)?.let {
            binding.message.text = it
        }

        arguments?.getString(ARG_HTML_MESSAGE)?.let {
            binding.message.setHtml(it)
        }

        arguments?.getString(ARG_POSITIVE)?.let {
            binding.positiveButton.text = it
        }

        arguments?.getString(ARG_NEGATIVE)?.let {
            binding.negativeButton.text = it
        }

        binding.positiveButton.setOnClickListener {
            setResult(RESULT_POSITIVE)
            dismiss()
        }

        binding.negativeButton.setOnClickListener {
            setResult(RESULT_NEGATIVE)
            dismiss()
        }

        return builder.create()
    }

    fun show(fragment: Fragment) {
        show(fragment.childFragmentManager, fragment)
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager, activity)
    }

    private fun setResult(result: String) {
        setFragmentResult(REQUEST_KEY, bundleOf(KEY_RESULT to result))
    }

    private fun show(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
        fragmentManager.setFragmentResultListener(
            REQUEST_KEY,
            lifecycleOwner
        ) { _, result ->
            when (result.getString(KEY_RESULT)) {
                RESULT_POSITIVE -> onPositiveClickListener?.onClick()
                RESULT_NEGATIVE -> onNegativeClickListener?.onClick()
            }
            childFragmentManager.clearFragmentResultListener(REQUEST_KEY)
        }
        show(fragmentManager, TAG)
    }
}
