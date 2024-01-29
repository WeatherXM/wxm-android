package com.weatherxm.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import com.weatherxm.R
import com.weatherxm.databinding.FragmentMessageDialogBinding
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.setVisible
import kotlinx.coroutines.launch
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class MessageDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentMessageDialogBinding

    private var title: String? = null
    private var message: String? = null
    private var htmlMessage: String? = null

    companion object {
        const val TAG = "MessageDialogFragment"
        const val ARG_TITLE = "title"
        const val ARG_MESSAGE = "message"
        const val ARG_HTML_MESSAGE = "html_message"

        fun newInstance(title: String?, message: String?, htmlMessage: String?) =
            MessageDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_HTML_MESSAGE, htmlMessage)
                }
            }
    }

    init {
        lifecycleScope.launch {
            withCreated {
                val args = requireArguments()
                title = args.getString(ARG_TITLE, null)
                message = args.getString(ARG_MESSAGE, null)
                htmlMessage = args.getString(ARG_HTML_MESSAGE, null)
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog_Layer1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessageDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title?.let {
            binding.title.text = it
        } ?: binding.title.setVisible(false)

        message?.let {
            binding.message.text = it
        }

        htmlMessage?.let {
            binding.message.setHtml(it)
            binding.message.movementMethod = BetterLinkMovementMethod.getInstance()
        }
    }
}
