package com.weatherxm.ui.components

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.weatherxm.R
import com.weatherxm.databinding.FragmentStationHealthExplanationBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.setHtml
import org.koin.android.ext.android.inject

class StationHealthExplanationDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentStationHealthExplanationBinding
    private val navigator: Navigator by inject()

    companion object {
        const val TAG = "StationHealthExplanationDialogFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStationHealthExplanationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.dataQualityExplanation.setHtml(R.string.station_health_data_quality_explanation)
        binding.locationQualityExplanation.setHtml(
            R.string.station_health_location_quality_explanation
        )

        binding.readMoreAction.setOnClickListener {
            navigator.openWebsite(context, getString(R.string.docs_url_qod_algorithm))
        }
    }

    fun show(fragment: Fragment) {
        show(fragment.childFragmentManager, TAG)
    }
}
