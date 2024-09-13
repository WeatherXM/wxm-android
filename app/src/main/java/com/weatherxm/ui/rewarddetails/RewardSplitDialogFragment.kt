package com.weatherxm.ui.rewarddetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
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

        model.getWalletAddress {
            val adapter = RewardSplitStakeholderAdapter(it.wallet, false)
            binding.stakeholderRecycler.adapter = adapter
            binding.message.text = getString(R.string.reward_split_desc, it.splits.size)
            adapter.submitList(it.splits)
        }

        binding.doneBtn.setOnClickListener {
            dismiss()
        }
    }

    fun show(activity: AppCompatActivity) {
        show(activity.supportFragmentManager, TAG)
    }
}
