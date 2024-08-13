package com.weatherxm.ui.rewarddetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.databinding.FragmentRewardSplitDialogBinding
import com.weatherxm.ui.common.RewardSplitStakeholderAdapter
import com.weatherxm.ui.components.BaseBottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class RewardSplitDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentRewardSplitDialogBinding

    private val model: RewardDetailsViewModel by activityViewModel()

    companion object {
        const val TAG = "RewardSplitDialogFragment"

        fun newInstance() = RewardSplitDialogFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRewardSplitDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.message.text =
            getString(R.string.reward_split_desc, model.getRewardSplitsData().splits.size)

        model.getWalletAddress {
            val adapter = RewardSplitStakeholderAdapter(it, false)
            binding.stakeholderRecycler.adapter = adapter
            adapter.submitList(model.getRewardSplitsData().splits)
        }

        binding.doneBtn.setOnClickListener {
            dismiss()
        }
    }
}
