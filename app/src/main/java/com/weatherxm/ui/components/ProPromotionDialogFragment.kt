package com.weatherxm.ui.components

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.FragmentProPromotionDialogBinding

class ProPromotionDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentProPromotionDialogBinding

    companion object {
        const val TAG = "ProPromotionDialogFragment"
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
        binding = FragmentProPromotionDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.learnMoreBtn.setOnClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.LEARN_MORE.paramValue,
                Pair(
                    FirebaseAnalytics.Param.ITEM_ID,
                    AnalyticsService.ParamValue.PRO_PROMOTION.paramValue
                )
            )
            navigator.openWebsiteExternally(context, getString(R.string.pro_url))
        }
    }

    fun show(fragment: Fragment) {
        show(fragment.childFragmentManager, TAG)
    }

    fun show(activity: AppCompatActivity) {
        show(activity.supportFragmentManager, TAG)
    }
}
