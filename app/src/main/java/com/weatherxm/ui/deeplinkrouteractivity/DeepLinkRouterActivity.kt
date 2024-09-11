package com.weatherxm.ui.deeplinkrouteractivity

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.databinding.ActivityDeepLinkRouterBinding
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeepLinkRouterActivity : BaseActivity() {
    private lateinit var binding: ActivityDeepLinkRouterBinding
    private val model: DeepLinkRouterViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeepLinkRouterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        model.onError().observe(this) {
            binding.logo.visible(false)
            binding.empty.clear()
                .animation(R.raw.anim_error)
                .title(R.string.parsing_failed)
                .subtitle(it)
                .visible(true)
        }

        model.onDevice().observe(this) {
            binding.logo.visible(false)
            val device = it.first
            val showExplorerOnBack = it.second
            if (showExplorerOnBack) {
                navigator.showDeviceDetails(this, device = device, openExplorerOnBack = true)
            } else {
                navigator.showDeviceDetailsWithBackStack(this, device)
            }
            finish()
        }

        model.onCell().observe(this) {
            binding.logo.visible(false)
            navigator.showCellInfo(this, it, openExplorerOnBack = true)
            finish()
        }

        model.onAnnouncement().observe(this) {
            binding.logo.visible(false)
            it?.let { url ->
                if (isTaskRoot) {
                    navigator.showStartup(this)
                }
                navigator.openWebsite(this, url)
            }
            finish()
        }

        model.parseIntent(intent)
    }
}

