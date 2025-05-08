package com.weatherxm.ui.explorer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.weatherxm.R
import com.weatherxm.databinding.FragmentMapLayerPickerDialogBinding
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
                MapLayer.DEFAULT -> {
                    check(binding.defaultOption, binding.defaultCard)
                    unCheck(binding.dataQualityOption, binding.dataQualityCard)
                }
                MapLayer.DATA_QUALITY -> {
                    check(binding.dataQualityOption, binding.dataQualityCard)
                    unCheck(binding.defaultOption, binding.defaultCard)
                }
                else -> throw IllegalArgumentException("Unknown map layer: $it")
            }
        }

        binding.defaultCard.setOnClickListener {
            model.setMapLayer(MapLayer.DEFAULT)
        }

        binding.dataQualityCard.setOnClickListener {
            model.setMapLayer(MapLayer.DATA_QUALITY)
        }
    }

    @Suppress("MagicNumber")
    private fun check(radioButton: RadioButton, cardContainer: MaterialCardView) {
        radioButton.isChecked = true
        with(cardContainer) {
            setCardStroke(R.color.colorPrimary, 3)
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
