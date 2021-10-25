package com.weatherxm.ui.home

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class HomeActivity : AppCompatActivity(), KoinComponent {
    private val model: HomeViewModel by viewModels()
    private val navigator: Navigator by inject()
    private val resourcesHelper: ResourcesHelper by inject()
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.logout) {
                model.logout()
                navigator.showSplash(this)
                finish()
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }

        val adapter = DeviceAdapter {
            model.selectDevice(it)
        }
        binding.recycler.adapter = adapter

        binding.swiperefresh.setOnRefreshListener {
            model.fetch()
        }

        model.devices().observe(this, {
            Timber.d("Data updated: ${it.status}")
            when (it.status) {
                Status.SUCCESS -> {
                    binding.swiperefresh.isRefreshing = false
                    if (!it.data.isNullOrEmpty()) {
                        adapter.submitList(it.data)
                        binding.recycler.visibility = View.VISIBLE
                        binding.empty.visibility = View.GONE
                        binding.progress.visibility = View.GONE
                    } else {
                        binding.empty.visibility = View.VISIBLE
                        binding.empty.title(resourcesHelper.getString(R.string.no_added_weather_stations_yet))
                        binding.empty.subtitle(resourcesHelper.getString(R.string.add_weather_station))
                        binding.empty.listener(null)
                        binding.recycler.visibility = View.GONE
                        binding.progress.visibility = View.GONE
                    }
                }
                Status.ERROR -> {
                    binding.swiperefresh.isRefreshing = false
                    binding.empty.title(resourcesHelper.getString(R.string.no_weather_stations_found))
                    binding.empty.subtitle(it.message)
                    binding.empty.action(resourcesHelper.getString(R.string.action_retry))
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

        model.fetch()
    }
}

fun interface OnDeviceSelectedListener {
    fun onDeviceSelected(device: Device)
}
