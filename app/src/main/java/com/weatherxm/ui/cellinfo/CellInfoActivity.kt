package com.weatherxm.ui.cellinfo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityCellInfoBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class CellInfoActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityCellInfoBinding
    private val model: CellInfoViewModel by viewModel {
        parametersOf(intent.getParcelableExtra<UICell>(Contracts.ARG_EXPLORER_CELL))
    }
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private lateinit var adapter: CellDeviceListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCellInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        adapter = CellDeviceListAdapter {
            navigator.showDeviceDetails(this, cellDevice = it, isUserDevice = false)
        }

        binding.recycler.adapter = adapter

        model.onCellDevices().observe(this) {
            updateUI(it)
        }

        model.address().observe(this) {
            binding.toolbar.subtitle = it
        }

        model.fetchDevices()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.EXPLORER_CELL,
            CellInfoActivity::class.simpleName,
            model.cell.index
        )
    }

    private fun updateUI(response: Resource<List<UIDevice>>) {
        when (response.status) {
            Status.SUCCESS -> {
                if (!response.data.isNullOrEmpty()) {
                    adapter.submitList(response.data)
                    binding.empty.visibility = View.GONE
                    binding.recycler.visibility = View.VISIBLE
                } else {
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_error)
                    binding.empty.title(getString(R.string.error_generic_message))
                    binding.recycler.visibility = View.GONE
                    binding.empty.visibility = View.VISIBLE
                }
            }
            Status.ERROR -> {
                Timber.d(response.message, response.message)
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_error)
                binding.empty.title(getString(R.string.error_generic_message))
                binding.empty.subtitle(response.message)
                binding.recycler.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE
            }
            Status.LOADING -> {
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.recycler.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE
            }
        }
    }
}
