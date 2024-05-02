package com.weatherxm.ui.claimdevice.selectdevicetype

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentClaimSelectDeviceTypeBinding
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.getClassSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseBottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectDeviceTypeDialogFragment : BaseBottomSheetDialogFragment() {

    fun interface OnDeviceTypeSelectedListener {
        fun onDeviceTypeSelected(type: DeviceType)
    }

    private val model: SelectDeviceTypeViewModel by viewModel()

    private lateinit var binding: FragmentClaimSelectDeviceTypeBinding
    private lateinit var adapter: AvailableDeviceTypesAdapter

    private var listener: OnDeviceTypeSelectedListener? = null

    companion object {
        const val TAG = "SelectDeviceTypeFragment"

        private const val REQUEST_KEY = "device_type_request_key"
        private const val KEY_RESULT = "device_type_result"

        fun newInstance(
            listener: OnDeviceTypeSelectedListener
        ) = SelectDeviceTypeDialogFragment().apply {
            this.listener = listener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimSelectDeviceTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make the recyclerview non-scrollable, since the whole bottom sheet will scroll
        binding.recycler.layoutManager = object : LinearLayoutManager(view.context) {
            override fun canScrollVertically() = false
        }

        adapter = AvailableDeviceTypesAdapter { type ->
            setResult(type)
        }

        binding.recycler.adapter = adapter

        binding.closePopup.setOnClickListener {
            dismiss()
        }

        adapter.submitList(model.getAvailableDeviceTypes())
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.CLAIM_DEVICE_TYPE_SELECTION, getClassSimpleName()
        )
    }

    fun show(fragment: Fragment) {
        show(fragment.childFragmentManager, fragment)
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager, activity)
    }

    private fun setResult(type: DeviceType) {
        setFragmentResult(REQUEST_KEY, Bundle().apply { putParcelable(KEY_RESULT, type) })
    }

    private fun show(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
        fragmentManager.setFragmentResultListener(
            REQUEST_KEY,
            lifecycleOwner
        ) { _, result ->
            listener?.let {
                result.parcelable<DeviceType>(KEY_RESULT)?.let { type ->
                    it.onDeviceTypeSelected(type)
                }
            }
            childFragmentManager.clearFragmentResultListener(REQUEST_KEY)
            dismiss()
        }
        show(fragmentManager, ActionDialogFragment.TAG)
    }
}
