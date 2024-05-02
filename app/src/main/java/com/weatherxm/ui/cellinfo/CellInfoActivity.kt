package com.weatherxm.ui.cellinfo

import android.os.Bundle
import androidx.activity.addCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityCellInfoBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_OPEN_EXPLORER_ON_BACK
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.getClassSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.explorer.UICell
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class CellInfoActivity : BaseActivity(), CellDeviceListener {
    private lateinit var binding: ActivityCellInfoBinding
    private val model: CellInfoViewModel by viewModel {
        parametersOf(intent.parcelable<UICell>(Contracts.ARG_EXPLORER_CELL))
    }
    private lateinit var adapter: CellDeviceListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCellInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        with(binding.toolbar) {
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            setOnMenuItemClickListener {
                return@setOnMenuItemClickListener if (it.itemId == R.id.share_cell) {
                    navigator.openShare(
                        this@CellInfoActivity,
                        getString(R.string.share_cell_url, model.cell.index)
                    )
                    true
                } else {
                    false
                }
            }
        }
        val openExplorerOnBack = intent.getBooleanExtra(ARG_OPEN_EXPLORER_ON_BACK, false)
        onBackPressedDispatcher.addCallback {
            if (!openExplorerOnBack || model.isLoggedIn() == null) {
                finish()
                return@addCallback
            }
            if (model.isLoggedIn() == true) {
                navigator.showHome(this@CellInfoActivity, model.cell.center)
            } else {
                navigator.showExplorer(this@CellInfoActivity, model.cell.center)
            }
            finish()
        }

        binding.capacityChip.setOnCloseIconClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.LEARN_MORE.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.INFO_CELL_CAPACITY.paramValue
                )
            )
            navigator.showMessageDialog(
                supportFragmentManager,
                getString(R.string.cell_capacity),
                getString(R.string.cell_capacity_explanation),
                readMoreUrl = getString(R.string.docs_url_cell_capacity),
                analyticsScreenName = AnalyticsService.Screen.CELL_CAPACITY_INFO.screenName
            )
        }

        val dialogOverlay = MaterialAlertDialogBuilder(this).create()

        adapter = CellDeviceListAdapter(this)

        binding.recycler.adapter = adapter

        model.onCellDevices().observe(this) {
            updateUI(it)
        }

        model.address().observe(this) {
            binding.title.text = it
            binding.title.setVisible(true)
        }

        model.onFollowStatus().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    binding.empty.setVisible(false)
                    dialogOverlay.cancel()
                }
                Status.ERROR -> {
                    toast(it.message ?: getString(R.string.error_reach_out_short))
                    binding.empty.setVisible(false)
                    dialogOverlay.cancel()
                }
                Status.LOADING -> {
                    binding.empty.animation(R.raw.anim_loading).setVisible(true)
                    dialogOverlay.show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.EXPLORER_CELL, getClassSimpleName(), model.cell.index
        )
        model.fetchDevices()
    }

    private fun updateUI(response: Resource<List<UIDevice>>) {
        when (response.status) {
            Status.SUCCESS -> {
                if (!response.data.isNullOrEmpty()) {
                    updateCellStats(response.data)
                    adapter.submitList(response.data)
                    binding.empty.setVisible(false)
                    binding.recycler.setVisible(true)
                } else {
                    binding.empty.clear()
                        .animation(R.raw.anim_error)
                        .title(getString(R.string.error_generic_message))
                    binding.recycler.setVisible(false)
                    binding.empty.setVisible(true)
                }
            }
            Status.ERROR -> {
                Timber.d(response.message, response.message)
                binding.empty.clear()
                    .animation(R.raw.anim_error)
                    .title(getString(R.string.error_generic_message))
                    .subtitle(response.message)
                binding.recycler.setVisible(false)
                binding.empty.setVisible(true)
            }
            Status.LOADING -> {
                binding.empty.clear().animation(R.raw.anim_loading)
                binding.recycler.setVisible(false)
                binding.empty.setVisible(true)
            }
        }
    }

    private fun updateCellStats(data: List<UIDevice>) {
        data.count { it.isOnline() }.apply {
            if (this > 1) {
                binding.activeChip.text = getString(R.string.cell_active_stations, this)
            } else if (this == 1) {
                binding.activeChip.text = getString(R.string.cell_active_station)
            } else {
                binding.activeChip.setVisible(false)
            }
        }
        binding.capacityChip.text = getString(R.string.cell_stations_present, data.size)
        binding.cellStatsContainer.setVisible(true)
    }

    override fun onDeviceClicked(device: UIDevice) {
        navigator.showDeviceDetails(this, device = device)
    }

    override fun onFollowBtnClicked(device: UIDevice) {
        if (model.isLoggedIn() == false) {
            navigator.showLoginDialog(
                fragmentActivity = this,
                title = getString(R.string.add_favorites),
                htmlMessage = getString(R.string.hidden_content_login_prompt, device.name)
            )
            return
        }

        if (device.isFollowed()) {
            navigator.showHandleFollowDialog(this, false, device.name) {
                model.unFollowStation(device.id)
            }
        } else if (device.isUnfollowed() && !device.isOnline()) {
            navigator.showHandleFollowDialog(this, true, device.name) {
                model.followStation(device.id)
            }
        } else if (device.isUnfollowed()) {
            model.followStation(device.id)
        }
    }
}
