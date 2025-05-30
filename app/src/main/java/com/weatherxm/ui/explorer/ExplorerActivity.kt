package com.weatherxm.ui.explorer

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import com.mapbox.geojson.Point
import com.weatherxm.BuildConfig
import com.weatherxm.data.models.Location
import com.weatherxm.databinding.ActivityExplorerBinding
import com.weatherxm.ui.common.Animation.HideAnimation.SlideOutToBottom
import com.weatherxm.ui.common.Animation.ShowAnimation.SlideInFromBottom
import com.weatherxm.ui.common.Contracts.ARG_CELL_CENTER
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.hide
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.show
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.BaseMapFragment
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.Locale

class ExplorerActivity : BaseActivity(), BaseMapFragment.OnMapDebugInfoListener {
    private val model: ExplorerViewModel by viewModel()
    private lateinit var binding: ActivityExplorerBinding

    init {
        lifecycleScope.launch {
            withCreated {
                requestNotificationsPermissions()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.overlayContainer.applyInsetter {
            type(navigationBars = true) {
                margin(left = false, top = false, right = false, bottom = true)
            }
        }

        model.onStatus().observe(this) { resource ->
            Timber.d("Status updated: ${resource.status}")
            when (resource.status) {
                Status.SUCCESS -> {
                    snackbar?.dismiss()
                }
                Status.ERROR -> {
                    Timber.d("Got error: $resource.message")
                    resource.message?.let {
                        showSnackbarMessage(binding.root, it, callback = { model.fetch() })
                    }
                }
                Status.LOADING -> {
                    snackbar?.dismiss()
                }
            }
        }

        model.onSearchOpenStatus().observe(this) { isOpened ->
            if (isOpened) {
                binding.overlayContainer.hide(SlideOutToBottom)
            } else {
                binding.overlayContainer.show(SlideInFromBottom)
            }
        }

        binding.mapLayerPickerBtn.setOnClickListener {
            MapLayerPickerDialogFragment().show(this)
        }

        binding.myLocationBtn.setOnClickListener {
            model.onMyLocation()
        }

        binding.login.setOnClickListener {
            navigator.showLogin(this)
        }

        binding.signupPrompt.setOnClickListener {
            navigator.showSignup(this)
        }

        model.showMapOverlayViews().observe(this) { shouldShow ->
            if (shouldShow) {
                binding.overlayContainer.show(SlideInFromBottom)
            } else {
                binding.overlayContainer.hide(SlideOutToBottom)
            }
        }

        binding.mapDebugInfoContainer.visible(BuildConfig.DEBUG)

        with(intent.parcelable<Location>(ARG_CELL_CENTER)) {
            this?.let {
                model.navigateToLocation(it)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Suppress("MagicNumber")
    override fun onMapDebugInfoUpdated(zoom: Double, center: Point) {
        fun format(number: Number, decimals: Int = 2): String {
            return String.format(Locale.getDefault(), "%.${decimals}f", number)
        }

        binding.mapDebugInfo.text = "ZOOM = ${format(zoom)}\nCENTER = " +
            "${format(center.latitude(), 6)}, ${format(center.longitude(), 6)}"
    }
}
