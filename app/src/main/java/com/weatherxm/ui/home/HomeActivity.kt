package com.weatherxm.ui.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.explorer.ExplorerViewModel
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.util.hideIfNot
import com.weatherxm.util.showIfNot
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class HomeActivity : AppCompatActivity(), KoinComponent {
    private val navigator: Navigator by inject()
    private lateinit var binding: ActivityHomeBinding
    private val explorerModel: ExplorerViewModel by viewModels()
    private val devicesViewModel: DevicesViewModel by viewModels()

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        explorerModel.onDeviceSelected().observe(this) { device ->
            navigator.showDeviceDetails(supportFragmentManager, device)
        }

        // todo: Find a way to use snackbar here instead of a toast
        explorerModel.explorerState().observe(this) { resource ->
            Timber.d("Status updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    snackbar?.dismiss()
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let { toast(it) }
                }
                Status.LOADING -> {
                    snackbar?.dismiss()
                }
            }
        }

        devicesViewModel.showOverlayViews().observe(this) { shouldShow ->
            if (shouldShow) {
                binding.addDevice.showIfNot()
                binding.navView.showIfNot()
            } else if (!shouldShow) {
                binding.addDevice.hideIfNot()
                binding.navView.hideIfNot()
            }
        }

        val navController = findNavController(R.id.nav_host_fragment)

        // Setup navigation view
        binding.navView.setupWithNavController(navController)

        // Show/hide FAB based on selected navigation item
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_devices -> binding.addDevice.showIfNot()
                else -> binding.addDevice.hideIfNot()
            }
        }

        // Disable BottomNavigationView bottom padding, added by default, and add margin
        // https://github.com/material-components/material-components-android/commit/276bec8385ec877548fc84994c0a016de2428567
        binding.navView.applyInsetter {
            type(navigationBars = true) {
                padding(horizontal = false, vertical = false)
                margin(bottom = true)
            }
        }
    }
}
