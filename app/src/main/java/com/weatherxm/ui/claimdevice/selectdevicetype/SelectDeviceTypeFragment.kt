package com.weatherxm.ui.claimdevice.selectdevicetype

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.databinding.FragmentClaimSelectDeviceTypeBinding
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.home.HomeViewModel

class SelectDeviceTypeFragment : BottomSheetDialogFragment() {
    private val model: SelectDeviceTypeViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimSelectDeviceTypeBinding
    private lateinit var adapter: AvailableDeviceTypesAdapter

    companion object {
        const val TAG = "SelectDeviceTypeFragment"
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

        adapter = AvailableDeviceTypesAdapter {
            if (it.type == DeviceType.HELIUM) {
                homeViewModel.claimHelium()
            } else {
                homeViewModel.claimM5()
            }
            dismiss()
        }

        binding.recycler.adapter = adapter

        binding.closePopup.setOnClickListener {
            dismiss()
        }

        adapter.submitList(model.getAvailableDeviceTypes())
    }
}
