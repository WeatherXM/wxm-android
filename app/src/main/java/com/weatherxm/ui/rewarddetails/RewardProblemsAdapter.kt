package com.weatherxm.ui.rewarddetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.RewardsAnnotationGroup
import com.weatherxm.data.Severity
import com.weatherxm.databinding.ListItemRewardProblemBinding
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.ui.common.UIDevice

class RewardProblemsAdapter(
    private val device: UIDevice,
    private val listener: RewardProblemsListener
) : ListAdapter<RewardsAnnotationGroup, RewardProblemsAdapter.RewardProblemsViewHolder>(
    RewardProblemsDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardProblemsViewHolder {
        val binding = ListItemRewardProblemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RewardProblemsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardProblemsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RewardProblemsViewHolder(private val binding: ListItemRewardProblemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RewardsAnnotationGroup) {
            with(binding.annotationContainer) {
                when (item.severity) {
                    Severity.INFO -> {
                        setBackground(R.color.blueTint)
                        setStrokeColor(R.color.colorPrimaryVariant)
                        setIcon(R.drawable.ic_info)
                        setIconColor(R.color.colorPrimaryVariant)
                    }
                    Severity.WARNING -> {
                        setBackground(R.color.warningTint)
                        setStrokeColor(R.color.warning)
                        setIcon(R.drawable.ic_warn)
                        setIconColor(R.color.warning)
                    }
                    Severity.ERROR -> {
                        setBackground(R.color.errorTint)
                        setStrokeColor(R.color.error)
                        setIcon(R.drawable.ic_warn)
                        setIconColor(R.color.error)
                    }
                    else -> {
                        setBackground(R.color.blueTint)
                        setStrokeColor(R.color.colorPrimaryVariant)
                        setIcon(R.drawable.ic_info)
                        setIconColor(R.color.colorPrimaryVariant)
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
                    binding.annotationContainer.action(getString(R.string.add_wallet_now)) {
                        listener.onAddWallet(code.toString())
                    }
                } else if (code == AnnotationGroupCode.LOCATION_NOT_VERIFIED && device.isOwned()) {
                    binding.annotationContainer.action(getString(R.string.edit_location)) {
                        listener.onEditLocation(device, code.toString())
                    }
                } else if (!docUrl.isNullOrEmpty()) {
                    binding.annotationContainer.action(getString(R.string.read_more)) {
                        listener.onDocumentation(docUrl, code.toString())
                    }
                } else {
                    // Do nothing
                }
            }
        }
    }
}

class RewardProblemsDiffCallback : DiffUtil.ItemCallback<RewardsAnnotationGroup>() {

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
            oldItem.severity == newItem.severity && oldItem.group == newItem.group &&
            oldItem.docUrl == newItem.docUrl
    }
}

interface RewardProblemsListener {
    fun onAddWallet(group: String?)
    fun onDocumentation(url: String, group: String?)
    fun onEditLocation(device: UIDevice, group: String?)
}
