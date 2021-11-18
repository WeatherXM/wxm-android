package com.weatherxm.ui.home.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDevicesBinding
import com.weatherxm.ui.Navigator
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DevicesFragment : Fragment(), KoinComponent, DeviceListener {

    private val model: DevicesViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private lateinit var binding: FragmentDevicesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDevicesBinding.inflate(inflater, container, false)

        binding.root.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }
        val adapter = DeviceAdapter(this)
        binding.recycler.adapter = adapter

        binding.swiperefresh.setOnRefreshListener {
            model.fetch()
        }

        model.devices().observe(viewLifecycleOwner, { devicesResource ->
            when (devicesResource.status) {
                Status.SUCCESS -> {
                    binding.swiperefresh.isRefreshing = false
                    if (!devicesResource.data.isNullOrEmpty()) {
                        adapter.submitList(devicesResource.data)
                        binding.recycler.visibility = View.VISIBLE
                        binding.empty.visibility = View.GONE
                        binding.progress.visibility = View.GONE
                    } else {
                        binding.empty.visibility = View.VISIBLE
                        binding.empty.title(
                            model.resHelper().getString(R.string.no_weather_stations)
                        )
                        binding.empty.subtitle(
                            model.resHelper().getString(R.string.add_weather_station)
                        )
                        binding.empty.listener(null)
                        binding.recycler.visibility = View.GONE
                        binding.progress.visibility = View.GONE
                    }
                }
                Status.ERROR -> {
                    binding.swiperefresh.isRefreshing = false
                    binding.empty.title(model.resHelper().getString(R.string.oops_something_wrong))
                    binding.empty.subtitle(devicesResource.message)
                    binding.empty.action(model.resHelper().getString(R.string.action_retry))
                    binding.empty.listener { model.fetch() }
                    binding.empty.visibility = View.VISIBLE
                    binding.recycler.visibility = View.GONE
                    binding.progress.visibility = View.GONE
                }
                Status.LOADING -> {
                    binding.progress.visibility =
                        if (binding.swiperefresh.isRefreshing) View.GONE else View.VISIBLE
                    binding.empty.visibility = View.GONE
                }
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch user's devices
        model.fetch()
    }

    override fun onDeviceClicked(device: Device) {
        navigator.showUserDevice(this, device)
    }
}
