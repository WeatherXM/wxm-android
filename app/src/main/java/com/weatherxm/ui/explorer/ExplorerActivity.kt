package com.weatherxm.ui.explorer

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityExplorerBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Animation.HideAnimation.SlideOutToBottom
import com.weatherxm.ui.common.Animation.HideAnimation.SlideOutToTop
import com.weatherxm.ui.common.Animation.ShowAnimation.SlideInFromBottom
import com.weatherxm.ui.common.Animation.ShowAnimation.SlideInFromTop
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.show
import com.weatherxm.util.ResourcesHelper
import dev.chrisbanes.insetter.applyInsetter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ExplorerActivity : AppCompatActivity(), KoinComponent {

    private val navigator: Navigator by inject()
    private val resourcesHelper: ResourcesHelper by inject()
    private val model: ExplorerViewModel by viewModels()
    private lateinit var binding: ActivityExplorerBinding

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyMapInsets()

        model.explorerState().observe(this, { resource ->
            Timber.d("Status updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    snackbar?.dismiss()
                    binding.progress.hide()
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    binding.progress.hide()
                    resource.message?.let { showErrorOnMapLoading(it) }
                }
                Status.LOADING -> {
                    snackbar?.dismiss()
                    binding.progress.show()
                }
            }
        })

        model.onDeviceSelected().observe(this, { device ->
            navigator.showDeviceDetails(supportFragmentManager, device)
        })

        model.showMapOverlayViews().observe(this, { shouldShow ->
            if (shouldShow) {
                binding.logo.show(SlideInFromTop)
                binding.settings.show(SlideInFromTop)
                binding.loginSignupView.show(SlideInFromBottom)
            } else {
                binding.logo.hide(SlideOutToTop)
                binding.settings.hide(SlideOutToTop)
                binding.loginSignupView.hide(SlideOutToBottom)
            }
        })

        binding.settings.setOnClickListener {
            navigator.showPreferences(this)
        }

        binding.login.setOnClickListener {
            navigator.showLogin(this)
        }

        binding.signupPrompt.text = HtmlCompat.fromHtml(
            resourcesHelper.getString(R.string.prompt_signup),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        binding.signupPrompt.setOnClickListener {
            navigator.showSignup(this)
        }
    }

    private fun showErrorOnMapLoading(message: String) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
        snackbar = Snackbar
            .make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.action_retry) {
                model.fetch()
            }
        snackbar?.show()
    }

    private fun applyMapInsets() {
        binding.logo.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

        binding.settings.applyInsetter {
            type(statusBars = true) {
                margin(left = false, top = true, right = false, bottom = false)
            }
        }

        binding.loginSignupView.applyInsetter {
            type(navigationBars = true) {
                margin(left = false, top = false, right = false, bottom = true)
            }
        }
    }
}
