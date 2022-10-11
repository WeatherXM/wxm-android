package com.weatherxm.ui.publicdeviceslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentPublicDevicesListBinding
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.ExplorerViewModel
import timber.log.Timber

class PublicDevicesListFragment : BottomSheetDialogFragment() {
    private val explorerModel: ExplorerViewModel by activityViewModels()
    private val model: PublicDevicesListViewModel by viewModels()
    private lateinit var binding: FragmentPublicDevicesListBinding
    private lateinit var adapter: PublicDevicesListAdapter

    companion object {
        const val TAG = "PublicDevicesListFragment"
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog_Explorer_Devices
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPublicDevicesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PublicDevicesListAdapter {
            explorerModel.onPublicDeviceClicked(explorerModel.getCurrentHexSelected(), it)
            dismiss()
        }

        binding.recycler.adapter = adapter

        model.onPublicDevices().observe(this) {
            updateUI(it)
        }

        model.address().observe(this) {
            binding.location.text = it
            binding.location.visibility = View.VISIBLE
        }

        model.fetchDevices(explorerModel.getCurrentHexSelected())
    }

    private fun updateUI(response: Resource<List<UIDevice>>) {
        when (response.status) {
            Status.SUCCESS -> {
                if (!response.data.isNullOrEmpty()) {
                    adapter.submitList(response.data)
                    binding.empty.visibility = View.GONE
                    binding.recycler.visibility = View.VISIBLE
                } else {
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_error)
                    binding.empty.title(getString(R.string.error_generic_message))
                    binding.recycler.visibility = View.GONE
                    binding.empty.visibility = View.VISIBLE
                }
            }
            Status.ERROR -> {
                Timber.d(response.message, response.message)
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_error)
                binding.empty.title(getString(R.string.error_generic_message))
                binding.empty.subtitle(response.message)
                binding.recycler.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE
            }
            Status.LOADING -> {
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.recycler.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE
            }
        }
    }
}
