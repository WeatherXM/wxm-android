package com.weatherxm.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.databinding.FragmentMessageDialogBinding
import kotlinx.coroutines.launch

class MessageDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentMessageDialogBinding

    private var title: String? = null
    private var message: String? = null

    companion object {
        const val TAG = "MessageDialogFragment"
        const val ARG_TITLE = "title"
        const val ARG_MESSAGE = "message"

        fun newInstance(title: String?, message: String?) = MessageDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
            }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                val args = requireArguments()
                if (args.containsKey(ARG_TITLE) && args.containsKey(ARG_MESSAGE)) {
                    title = args.getString(ARG_TITLE)
                    message = args.getString(ARG_MESSAGE)
                }
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
        }

        message?.let {
            binding.message.text = it
        }
    }
}
