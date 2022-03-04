package com.weatherxm.ui.token

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.data.Transaction
import com.weatherxm.databinding.ActivityTokenBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.token.TokenViewModel.Companion.TransactionExplorer
import com.weatherxm.util.applyInsets
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class TokenActivity : AppCompatActivity(), KoinComponent {

    private lateinit var binding: ActivityTokenBinding
    private val model: TokenViewModel by viewModels()
    private val navigator: Navigator by inject()

    private lateinit var adapter: TransactionsAdapter
    private lateinit var deviceId: String

    companion object {
        const val ARG_DEVICE = "device"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val device = intent?.extras?.getParcelable<Device>(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start TokenActivity. Device is null.")
            toast(R.string.unknown_error)
            finish()
            return
        }

        deviceId = device.id
        binding.toolbar.subtitle = device.name

        // Initialize the adapter with empty data
        adapter = TransactionsAdapter {
            it.tx_hash?.let { hash ->
                navigator.openWebsite(this, "$TransactionExplorer$hash")
            }
        }
        binding.recycler.adapter = adapter

        model.onTransactions().observe(this) {
            updateUI(it)
        }

        model.fetchTransactions(deviceId)
    }

    private fun updateUI(resource: Resource<List<Transaction>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                if (!resource.data.isNullOrEmpty()) {
                    adapter.submitList(resource.data)
                    binding.recycler.visibility = View.VISIBLE
                    binding.empty.visibility = View.GONE
                } else {
                    binding.empty.animation(R.raw.anim_empty_devices, false)
                    binding.empty.title(getString(R.string.no_transactions))
                    binding.empty.subtitle(getString(R.string.come_back_later))
                    binding.empty.listener(null)
                    binding.empty.visibility = View.VISIBLE
                    binding.recycler.visibility = View.GONE
                }
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                binding.recycler.visibility = View.GONE
                binding.empty.animation(R.raw.anim_error)
                binding.empty.title(getString(R.string.no_transactions_data))
                binding.empty.subtitle(resource.message)
                binding.empty.action(getString(R.string.action_retry))
                binding.empty.listener { model.fetchTransactions(deviceId) }
                binding.empty.visibility = View.VISIBLE
            }
            Status.LOADING -> {
                binding.recycler.visibility = View.GONE
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.empty.visibility = View.VISIBLE
            }
        }
    }
}

