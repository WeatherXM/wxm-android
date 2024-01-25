package com.weatherxm.ui.urlrouteractivity

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.data.WXMRemoteMessage
import com.weatherxm.databinding.ActivityUrlRouterBinding
import com.weatherxm.ui.common.Contracts.ARG_REMOTE_MESSAGE
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.parcelable
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

        // TODO: Handle remote message
        val remoteMessage = intent.parcelable<WXMRemoteMessage>(ARG_REMOTE_MESSAGE)

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

