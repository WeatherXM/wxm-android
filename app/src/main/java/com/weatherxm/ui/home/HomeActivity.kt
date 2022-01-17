package com.weatherxm.ui.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.weatherxm.R
import com.weatherxm.databinding.ActivityHomeBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.explorer.ExplorerViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeActivity : AppCompatActivity(), KoinComponent {
    private val navigator: Navigator by inject()
    private lateinit var binding: ActivityHomeBinding
    private val explorerModel: ExplorerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        explorerModel.onDeviceSelected().observe(this, { device ->
            navigator.showDeviceDetails(supportFragmentManager, device)
        })

        binding.navView.setupWithNavController(findNavController(R.id.nav_host_fragment))
    }
}
