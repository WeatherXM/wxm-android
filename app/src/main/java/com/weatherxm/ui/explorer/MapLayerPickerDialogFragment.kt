package com.weatherxm.ui.explorer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentMapLayerPickerDialogBinding
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.components.BaseBottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MapLayerPickerDialogFragment : BaseBottomSheetDialogFragment() {

    private lateinit var binding: FragmentMapLayerPickerDialogBinding
    private val model: ExplorerViewModel by activityViewModel()

    companion object {
        const val TAG = "MapLayerPickerDialogFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapLayerPickerDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.onMapLayer().observe(this) {
            when (it) {
                MapLayer.DENSITY -> {
                    check(binding.densityOption, binding.densityCard)
                    unCheck(binding.dataQualityOption, binding.dataQualityCard)
                }
                MapLayer.DATA_QUALITY -> {
                    check(binding.dataQualityOption, binding.dataQualityCard)
                    unCheck(binding.densityOption, binding.densityCard)
                }
                else -> throw IllegalArgumentException("Unknown map layer: $it")
            }
        }

        binding.densityCard.setOnClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.SELECT_MAP_LAYER.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.DENSITY.paramValue
                )
            )
            model.setMapLayer(MapLayer.DENSITY)
            dismiss()
        }

        binding.dataQualityCard.setOnClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.SELECT_MAP_LAYER.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.DATA_QUALITY.paramValue
                )
            )
            model.setMapLayer(MapLayer.DATA_QUALITY)
            dismiss()
        }

        analytics.trackScreen(AnalyticsService.Screen.MAP_LAYER_PICKER, classSimpleName())
    }

    @Suppress("MagicNumber")
    private fun check(radioButton: RadioButton, cardContainer: MaterialCardView) {
        radioButton.isChecked = true
        with(cardContainer) {
            setCardStroke(R.color.colorPrimary, 5)
            setCardBackgroundColor(this.context.getColor(R.color.layer2))
        }
    }

    private fun unCheck(radioButton: RadioButton, cardContainer: MaterialCardView) {
        radioButton.isChecked = false
        with(cardContainer) {
            strokeWidth = 0
            setCardBackgroundColor(this.context.getColor(R.color.layer1))
        }
    }

    fun show(activity: AppCompatActivity) {
        show(activity.supportFragmentManager, TAG)
    }
}
