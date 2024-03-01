package com.weatherxm.ui.rewardissues

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.RewardsAnnotationGroup
import com.weatherxm.data.SeverityLevel
import com.weatherxm.databinding.ListItemRewardIssueBinding
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.UIDevice

class RewardIssuesAdapter(
    private val device: UIDevice,
    private val listener: RewardIssuesListener
) : ListAdapter<RewardsAnnotationGroup, RewardIssuesAdapter.RewardIssuesViewHolder>(
    RewardIssuesDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardIssuesViewHolder {
        val binding = ListItemRewardIssueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RewardIssuesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardIssuesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RewardIssuesViewHolder(private val binding: ListItemRewardIssueBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RewardsAnnotationGroup) {
            with(binding.annotationCard) {
                when (item.severityLevel) {
                    SeverityLevel.INFO -> {
                        setBackground(R.color.blueTint)
                        setStrokeColor(R.color.light_lightest_blue)
                    }
                    SeverityLevel.WARNING -> {
                        setBackground(R.color.warningTint)
                        setStrokeColor(R.color.warning)
                    }
                    SeverityLevel.ERROR -> {
                        setBackground(R.color.errorTint)
                        setStrokeColor(R.color.error)
                    }
                    else -> {
                        setBackground(R.color.blueTint)
                        setStrokeColor(R.color.light_lightest_blue)
                    }
                }
                title(item.title)
                message(item.message)
                setAction(item.toAnnotationGroupCode(), item.docUrl)
            }
        }

        private fun setAction(code: AnnotationGroupCode?, docUrl: String?) {
            with(itemView.context) {
                if (code == AnnotationGroupCode.NO_WALLET && device.isOwned()) {
                    binding.annotationCard.action(getString(R.string.add_wallet_now)) {
                        listener.onAddWallet(code.toString())
                    }
                } else if (code == AnnotationGroupCode.LOCATION_NOT_VERIFIED && device.isOwned()) {
                    binding.annotationCard.action(getString(R.string.edit_location)) {
                        listener.onEditLocation(device, code.toString())
                    }
                } else if (!docUrl.isNullOrEmpty()) {
                    binding.annotationCard.action(getString(R.string.read_more)) {
                        listener.onDocumentation(docUrl, code.toString())
                    }
                } else {
                    // Do nothing
                }
            }
        }
    }
}

class RewardIssuesDiffCallback : DiffUtil.ItemCallback<RewardsAnnotationGroup>() {

    override fun areItemsTheSame(
        oldItem: RewardsAnnotationGroup,
        newItem: RewardsAnnotationGroup
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: RewardsAnnotationGroup,
        newItem: RewardsAnnotationGroup
    ): Boolean {
        return oldItem.title == newItem.title && oldItem.message == newItem.message &&
            oldItem.severityLevel == newItem.severityLevel && oldItem.group == newItem.group &&
            oldItem.docUrl == newItem.docUrl
    }
}

interface RewardIssuesListener {
    fun onAddWallet(group: String?)
    fun onDocumentation(url: String, group: String?)
    fun onEditLocation(device: UIDevice, group: String?)
}
