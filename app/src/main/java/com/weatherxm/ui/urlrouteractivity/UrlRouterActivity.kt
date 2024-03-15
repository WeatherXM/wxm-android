package com.weatherxm.ui.urlrouteractivity

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.databinding.ActivityUrlRouterBinding
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class UrlRouterActivity : BaseActivity() {
    private lateinit var binding: ActivityUrlRouterBinding
    private val model: UrlRouterViewModel by viewModel()

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

        model.onCell().observe(this) {
            binding.logo.setVisible(false)
            navigator.showCellInfo(this, it, openExplorerOnBack = true)
            finish()
        }

        model.onRemoteMessage().observe(this) {
            binding.logo.setVisible(false)
            it.url?.let { url ->
                if (isTaskRoot) {
                    navigator.showStartup(this)
                }
                navigator.openWebsite(this, url)
            }
            finish()
        }

        model.parseUrl(intent)
    }
}

