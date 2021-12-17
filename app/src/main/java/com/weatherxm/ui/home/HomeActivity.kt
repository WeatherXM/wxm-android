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
import com.weatherxm.ui.home.profile.ProfileViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class HomeActivity : AppCompatActivity(), KoinComponent {
    private val navigator: Navigator by inject()
    private lateinit var binding: ActivityHomeBinding
    private val explorerModel: ExplorerViewModel by viewModels()
    private val profileModel: ProfileViewModel by viewModels()
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        explorerModel.onDeviceSelected().observe(this, { device ->
            navigator.showDeviceDetails(supportFragmentManager, device)
        })

        profileModel.user().observe(this, { resource ->
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
        })

        binding.navView.setupWithNavController(findNavController(R.id.nav_host_fragment))
    }
}
