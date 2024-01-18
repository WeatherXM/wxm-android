package com.weatherxm.ui.passwordprompt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentPasswordPromptBinding
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseBottomSheetDialogFragment
import com.weatherxm.util.Analytics
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class PasswordPromptFragment : BaseBottomSheetDialogFragment() {

    fun interface OnPasswordConfirmedListener {
        fun onPasswordConfirmed(confirmed: Boolean)
    }

    private lateinit var binding: FragmentPasswordPromptBinding
    private val model: PasswordPromptViewModel by viewModel()

    private var messageResId: Int? = null

    companion object {
        const val TAG = "PasswordPromptFragment"
        private const val REQUEST_KEY = "password_confirm_request_key"
        private const val RESULT_KEY = "password_confirm_result_key"
        private const val ARG_MESSAGE_RES_ID = "message_res_id"

        fun newInstance(@StringRes message: Int) = PasswordPromptFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_MESSAGE_RES_ID, message)
            }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                if (requireArguments().containsKey(ARG_MESSAGE_RES_ID)) {
                    messageResId = requireArguments().getInt(ARG_MESSAGE_RES_ID)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPasswordPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageResId?.let {
            binding.description.setHtml(it)
        }

        binding.password.onTextChanged {
            binding.passwordContainer.error = null
            binding.actionButton.isEnabled = it.isNotEmpty()
        }

        binding.actionButton.setOnClickListener {
            model.checkPassword(binding.password.text.toString().trim())
        }

        model.onValidPassword().observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it.status) {
                    Status.SUCCESS -> {
                        dismiss()
                        setResult(true)
                    }
                    Status.ERROR -> {
                        setResult(false)
                        binding.passwordContainer.error = it.message
                    }
                    Status.LOADING -> {
                        // TODO Show progress bar?
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.PASSWORD_CONFIRM,
            PasswordPromptFragment::class.simpleName
        )
    }

    private fun setResult(result: Boolean) {
        setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to result))
    }

    private fun show(
        fragmentManager: FragmentManager,
        lifecycleOwner: LifecycleOwner,
        listener: OnPasswordConfirmedListener? = null
    ) {
        // Listen for fragment result
        fragmentManager.setFragmentResultListener(
            REQUEST_KEY,
            lifecycleOwner
        ) { _, result ->
            // Clear result listener for next instances
            fragmentManager.clearFragmentResultListener(REQUEST_KEY)

            // Propagate result to listener
            listener?.let {
                val isConfirmed = result.getBoolean(RESULT_KEY, false)
                it.onPasswordConfirmed(isConfirmed)
            }
        }

        show(fragmentManager, TAG)
    }

    fun show(
        activity: FragmentActivity,
        onPasswordConfirmedListener: OnPasswordConfirmedListener? = null
    ) {
        show(activity.supportFragmentManager, activity, onPasswordConfirmedListener)
    }
}
