package com.weatherxm.ui.urlrouteractivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Location
import com.weatherxm.databinding.ActivityUrlRouterBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UrlRouterActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityUrlRouterBinding
    private val navigator: Navigator by inject()
    private val model: UrlRouterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUrlRouterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        model.onError().observe(this) {
            binding.logo.setVisible(false)
            binding.empty.clear()
                .animation(R.raw.anim_error)
                .title(R.string.parsing_url_failed)
                .subtitle(it)
                .setVisible(true)
        }

        model.onDevice().observe(this) {
            binding.logo.setVisible(false)
            navigator.showDeviceDetails(
                this,
                device = it,
                openExplorerOnBack = true
            )
            finish()
        }

        model.parseUrl(intent.data)
    }
}

