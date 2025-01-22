package com.weatherxm.ui.rewardissues

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.models.RewardsAnnotationGroup
import com.weatherxm.databinding.ListItemRewardIssueBinding
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.components.compose.RewardIssueView

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
            val (actionLabel, actionToRun) = getAction(
                item.toAnnotationGroupCode(),
                item.docUrl
            )?.run {
                first to second
            } ?: (null to null)

            binding.annotationCard.setContent {
                RewardIssueView(
                    title = item.title,
                    subtitle = item.message,
                    action = actionLabel,
                    severityLevel = item.severityLevel,
                    onClickListener = actionToRun
                )
            }
        }

        private fun getAction(
            code: AnnotationGroupCode?,
            docUrl: String?
        ): Pair<String, () -> Unit>? {
            return with(itemView.context) {
                if (code == AnnotationGroupCode.NO_WALLET && device.isOwned()) {
                    Pair(
                        getString(R.string.add_wallet_now)
                    ) { listener.onAddWallet(code.toString()) }
                } else if (code == AnnotationGroupCode.LOCATION_NOT_VERIFIED && device.isOwned()) {
                    Pair(
                        getString(R.string.edit_location)
                    ) { listener.onEditLocation(device, code.toString()) }
                } else if (!docUrl.isNullOrEmpty()) {
                    Pair(
                        getString(R.string.read_more)
                    ) { listener.onDocumentation(docUrl, code.toString()) }
                } else {
                    null
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
