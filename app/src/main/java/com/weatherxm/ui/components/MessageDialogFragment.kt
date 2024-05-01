package com.weatherxm.ui.components

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.weatherxm.R
import com.weatherxm.databinding.FragmentMessageDialogBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.getClassSimpleName
import com.weatherxm.ui.common.setVisible
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MessageDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentMessageDialogBinding
    private val navigator: Navigator by inject()

    private var title: String? = null
    private var message: String? = null
    private var readMoreUrl: String? = null
    private var analyticsScreenName: String? = null

    companion object {
        const val TAG = "MessageDialogFragment"
        const val ARG_TITLE = "title"
        const val ARG_MESSAGE = "message"
        const val ARG_READ_MORE_URL = "read_more_url"
        const val ARG_ANALYTICS_SCREEN_NAME = "analytics_screen_name"

        fun newInstance(
            title: String?,
            message: String?,
            readMoreUrl: String?,
            analyticsScreenName: String?
        ) =
            MessageDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_READ_MORE_URL, readMoreUrl)
                    putString(ARG_ANALYTICS_SCREEN_NAME, analyticsScreenName)
                }
            }
    }

    init {
        lifecycleScope.launch {
            withCreated {
                val args = requireArguments()
                title = args.getString(ARG_TITLE, null)
                message = args.getString(ARG_MESSAGE, null)
                readMoreUrl = args.getString(ARG_READ_MORE_URL, null)
                analyticsScreenName = args.getString(ARG_ANALYTICS_SCREEN_NAME, null)
            }
        }
    }

    override fun getTheme(): Int = R.style.ThemeOverlay_WeatherXM_BottomSheetDialog_Layer1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
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

        readMoreUrl?.let { url ->
            binding.readMoreAction.setOnClickListener {
                navigator.openWebsite(context, url)
            }
            binding.readMoreAction.setVisible(true)
        }
    }

    override fun onResume() {
        super.onResume()
        analyticsScreenName?.let {
            analytics.trackScreen(it, getClassSimpleName())
        }
    }
}
