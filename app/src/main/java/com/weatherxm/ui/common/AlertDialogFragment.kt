package com.weatherxm.ui.common

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

class AlertDialogFragment : DialogFragment() {

    fun interface OnDialogActionClickListener {
        fun onClick()
    }

    @Suppress("LongParameterList")
    class Builder(
        var title: String? = null,
        var message: CharSequence? = null,
        var positive: String? = null,
        var negative: String? = null,
        var neutral: String? = null,
        var view: View? = null,
        var onPositiveClickListener: OnDialogActionClickListener? = null,
        var onNegativeClickListener: OnDialogActionClickListener? = null,
        var onNeutralClickListener: OnDialogActionClickListener? = null,
        var cancellable: Boolean? = true
    ) {
        fun title(title: String) = apply {
            this.title = title
        }

        fun message(message: CharSequence) = apply {
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

        fun onNeutralClick(label: String, listener: OnDialogActionClickListener): Builder = apply {
            this.neutral = label
            this.onNeutralClickListener = listener
        }

        fun cancellable(cancellable: Boolean): Builder = apply {
            this.cancellable = cancellable
        }

        fun build(): AlertDialogFragment {
            Timber.d("Building AlertDialogFragment with params: $this")
            return AlertDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to this@Builder.title,
                    ARG_MESSAGE to this@Builder.message,
                    ARG_POSITIVE to this@Builder.positive,
                    ARG_NEGATIVE to this@Builder.negative,
                    ARG_NEUTRAL to this@Builder.neutral,
                    ARG_CANCELLABLE to this@Builder.cancellable
                )
                customView = this@Builder.view
                onPositiveClickListener = this@Builder.onPositiveClickListener
                onNegativeClickListener = this@Builder.onNegativeClickListener
                onNeutralClickListener = this@Builder.onNeutralClickListener
            }
        }
    }

    companion object {
        const val TAG = "AlertDialogFragment"

        private const val REQUEST_KEY = "alert_dialog_request_key"
        private const val KEY_RESULT = "result"

        private const val RESULT_POSITIVE = "positive"
        private const val RESULT_NEGATIVE = "negative"
        private const val RESULT_NEUTRAL = "neutral"

        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE = "positive"
        private const val ARG_NEUTRAL = "neutral"
        private const val ARG_NEGATIVE = "negative"
        private const val ARG_CANCELLABLE = "cancellable"
    }

    private var onPositiveClickListener: OnDialogActionClickListener? = null
    private var onNegativeClickListener: OnDialogActionClickListener? = null
    private var onNeutralClickListener: OnDialogActionClickListener? = null
    private var customView: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        arguments?.getString(ARG_TITLE)?.let {
            builder.setTitle(it)
        }

        arguments?.getCharSequence(ARG_MESSAGE)?.let {
            builder.setMessage(it)
        }

        arguments?.getBoolean(ARG_CANCELLABLE)?.let {
            builder.setCancelable(it)
        }

        val listener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> setResult(RESULT_POSITIVE)
                DialogInterface.BUTTON_NEGATIVE -> setResult(RESULT_NEGATIVE)
                DialogInterface.BUTTON_NEUTRAL -> setResult(RESULT_NEUTRAL)
            }
        }

        arguments?.getString(ARG_POSITIVE)?.let {
            builder.setPositiveButton(it, listener)
        }
        arguments?.getString(ARG_NEGATIVE)?.let {
            builder.setNegativeButton(it, listener)
        }
        arguments?.getString(ARG_NEUTRAL)?.let {
            builder.setNeutralButton(it, listener)
        }

        customView?.let {
            builder.setView(it)
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
                RESULT_NEUTRAL -> onNeutralClickListener?.onClick()
            }
            childFragmentManager.clearFragmentResultListener(REQUEST_KEY)
        }
        show(fragmentManager, TAG)
    }
}
