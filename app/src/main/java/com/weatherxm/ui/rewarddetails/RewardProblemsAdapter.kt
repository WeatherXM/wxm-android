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
                item.annotation?.getTitleResId()?.let {
                    title(it)
                }
                htmlMessage(item.getMessage(context, device))
                if (device.relation == DeviceRelation.OWNED) {
                    getAction(item.annotation)
                }
            }
        }

        private fun getAction(annotation: AnnotationCode?) {
            with(itemView.context) {
                if (annotation == AnnotationCode.OBC && device.needsUpdate()) {
                    binding.error.action(getString(R.string.action_update_firmware)) {
                        listener.onAddWallet(annotation)
                    }
                } else if (annotation.pointToDocsHome()) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(getString(R.string.docs_url), annotation)
                    }
                } else if (annotation.pointToDocsTroubleshooting()) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(
                            getString(R.string.docs_url_troubleshooting),
                            annotation
                        )
                    }
                } else if (annotation == AnnotationCode.NO_WALLET) {
                    binding.error.action(getString(R.string.add_wallet_now)) {
                        listener.onUpdateFirmware(device, annotation)
                    }
                } else if (annotation == AnnotationCode.CELL_CAPACITY_REACHED) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(
                            getString(R.string.docs_url_cell_capacity),
                            annotation
                        )
                    }
                } else if (annotation == AnnotationCode.POL_THRESHOLD_NOT_REACHED) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(
                            getString(R.string.docs_url_pol_algorithm),
                            annotation
                        )
                    }
                } else if (annotation == AnnotationCode.QOD_THRESHOLD_NOT_REACHED) {
                    binding.error.action(getString(R.string.read_more)) {
                        listener.onDocumentation(
                            getString(R.string.docs_url_qod_algorithm),
                            annotation
                        )
                    }
                } else if (annotation == AnnotationCode.UNKNOWN) {
                    binding.error.action(getString(R.string.title_contact_support)) {
                        listener.onContactSupport(device, annotation)
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
    fun onAddWallet(annotation: AnnotationCode?)
    fun onUpdateFirmware(device: UIDevice, annotation: AnnotationCode?)
    fun onContactSupport(device: UIDevice, annotation: AnnotationCode?)
    fun onDocumentation(url: String, annotation: AnnotationCode?)
}
