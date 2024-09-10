package com.weatherxm.ui.devicedetails.forecast

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.weatherxm.databinding.FragmentTemperatureBarExplanationDialogBinding
import com.weatherxm.ui.components.BaseBottomSheetDialogFragment

class TemperatureBarExplanationDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentTemperatureBarExplanationDialogBinding

    companion object {
        const val TAG = "TemperatureBarExplanationDialogFragment"
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
        binding = FragmentTemperatureBarExplanationDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun show(fragment: Fragment) {
        show(fragment.childFragmentManager, TAG)
    }
}
