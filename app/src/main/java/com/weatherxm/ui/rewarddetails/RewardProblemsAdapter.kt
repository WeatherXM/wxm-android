package com.weatherxm.ui.rewarddetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemRewardProblemBinding
import com.weatherxm.ui.common.AnnotationCode
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIRewardsAnnotation
import com.weatherxm.util.Rewards.getMessage
import com.weatherxm.util.Rewards.getRewardAnnotationBackgroundColor
import com.weatherxm.util.Rewards.getRewardAnnotationColor
import com.weatherxm.util.Rewards.getTitleResId
import com.weatherxm.util.Rewards.pointToDocsHome
import com.weatherxm.util.Rewards.pointToDocsTroubleshooting

class RewardProblemsAdapter(
    private val device: UIDevice,
    private val rewardScore: Int?,
    private val listener: RewardProblemsListener
) : ListAdapter<UIRewardsAnnotation, RewardProblemsAdapter.RewardProblemsViewHolder>(
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

        fun bind(item: UIRewardsAnnotation) {
            with(binding.error) {
                setBackground(getRewardAnnotationBackgroundColor(rewardScore))
                val annotationColor = getRewardAnnotationColor(rewardScore)
                setIconColor(annotationColor)
                setStrokeColor(annotationColor)
                val annotationTitle = item.annotation?.getTitleResId()?.let {
                    title(it)
                    context.getString(it)
                } ?: ""
                htmlMessage(item.getMessage(context, device)) {
                    val docsUrlResId = when (item.annotation) {
                        AnnotationCode.POL_THRESHOLD_NOT_REACHED -> R.string.docs_url_pol_algorithm
                        AnnotationCode.QOD_THRESHOLD_NOT_REACHED -> R.string.docs_url_qod_algorithm
                        else -> R.string.docs_url
                    }
                    listener.onDocumentation(context.getString(docsUrlResId))
                }

                if (device.relation == DeviceRelation.OWNED) {
                    getAction(item.annotation, annotationTitle)
                }
            }
        }

        private fun getAction(annotation: AnnotationCode?, annotationTitle: String) {
            with(itemView.context) {
                if (annotation == AnnotationCode.OBC && device.needsUpdate()) {
                    binding.error.action(getString(R.string.add_wallet_now)) {
                        listener.onAddWallet()
                    }
                } else if (annotation.pointToDocsHome()) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(getString(R.string.docs_url))
                    }
                } else if (annotation.pointToDocsTroubleshooting()) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(getString(R.string.docs_url_troubleshooting))
                    }
                } else if (annotation == AnnotationCode.NO_WALLET) {
                    binding.error.action(getString(R.string.action_update_firmware)) {
                        listener.onUpdateFirmware(device)
                    }
                } else if (annotation == AnnotationCode.CELL_CAPACITY_REACHED) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(getString(R.string.docs_url_cell_capacity))
                    }
                } else if (annotation == AnnotationCode.POL_THRESHOLD_NOT_REACHED) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(getString(R.string.docs_url_pol_algorithm))
                    }
                } else if (annotation == AnnotationCode.QOD_THRESHOLD_NOT_REACHED) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(getString(R.string.docs_url_qod_algorithm))
                    }
                } else if (annotation == AnnotationCode.UNKNOWN) {
                    binding.error.action(getString(R.string.title_contact_support)) {
                        listener.onContactSupport(device, annotationTitle)
                    }
                } else {
                    // Do nothing, no action should be set.
                }
            }
        }
    }
}

class RewardProblemsDiffCallback : DiffUtil.ItemCallback<UIRewardsAnnotation>() {

    override fun areItemsTheSame(
        oldItem: UIRewardsAnnotation,
        newItem: UIRewardsAnnotation
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: UIRewardsAnnotation,
        newItem: UIRewardsAnnotation
    ): Boolean {
        return oldItem.annotation == newItem.annotation &&
            oldItem.ratioOfAnnotation == newItem.ratioOfAnnotation &&
            oldItem.qodParametersAffected == newItem.qodParametersAffected
    }
}

interface RewardProblemsListener {
    fun onAddWallet()
    fun onUpdateFirmware(device: UIDevice)
    fun onContactSupport(device: UIDevice, annotationTitle: String)
    fun onDocumentation(url: String)
}
