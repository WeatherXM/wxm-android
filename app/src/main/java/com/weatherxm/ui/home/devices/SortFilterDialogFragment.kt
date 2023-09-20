package com.weatherxm.ui.home.devices

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.databinding.FragmentDevicesSortFilterBinding
import com.weatherxm.ui.common.ActionDialogFragment
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.util.Analytics
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

class SortFilterDialogFragment : BottomSheetDialogFragment(), KoinComponent {

    private val model: DevicesViewModel by activityViewModels()
    private val analytics: Analytics by inject()

    private lateinit var binding: FragmentDevicesSortFilterBinding

    companion object {
        const val TAG = "SortFilterDialogFragment"

        fun newInstance() = SortFilterDialogFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDevicesSortFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelBtn.setOnClickListener {
            dismiss()
        }

        binding.resetBtn.setOnClickListener {
            binding.sortButtons.check(R.id.dateAdded)
            binding.filterButtons.check(R.id.showAll)
            binding.groupButtons.check(R.id.noGrouping)
        }

        binding.saveBtn.setOnClickListener {
            model.setDevicesSortFilterOptions(
                binding.sortButtons.checkedRadioButtonId,
                binding.filterButtons.checkedRadioButtonId,
                binding.groupButtons.checkedRadioButtonId
            )
            model.fetch()
            dismiss()
        }

        model.getDevicesSortFilterOptions().apply {
            binding.sortButtons.check(
                when (this.sortOrder) {
                    DevicesSortOrder.DATE_ADDED -> R.id.dateAdded
                    DevicesSortOrder.NAME -> R.id.name
                    DevicesSortOrder.LAST_ACTIVE -> R.id.lastActive
                }
            )
            binding.filterButtons.check(
                when (this.filterType) {
                    DevicesFilterType.ALL -> R.id.showAll
                    DevicesFilterType.OWNED -> R.id.ownedOnly
                    DevicesFilterType.FAVORITES -> R.id.favoritesOnly
                }
            )
            binding.groupButtons.check(
                when (this.groupBy) {
                    DevicesGroupBy.NO_GROUPING -> R.id.noGrouping
                    DevicesGroupBy.RELATIONSHIP -> R.id.relationship
                    DevicesGroupBy.STATUS -> R.id.status
                }
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.SORT_FILTER_DEVICES_OPTIONS,
            SortFilterDialogFragment::class.simpleName
        )
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog
    }

    fun show(fragment: Fragment) {
        show(fragment.childFragmentManager, ActionDialogFragment.TAG)
    }
}
