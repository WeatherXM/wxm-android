package com.weatherxm.ui.photoverification

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemPhotoExampleBinding
import com.weatherxm.ui.common.PhotoExample
import com.weatherxm.ui.common.visible

class PhotoExampleAdapter :
    ListAdapter<PhotoExample, PhotoExampleAdapter.PhotoExampleAdapterViewHolder>(
        PhotoExampleAdapterDiffCallback()
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PhotoExampleAdapterViewHolder {
        val binding = ListItemPhotoExampleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoExampleAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoExampleAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhotoExampleAdapterViewHolder(
        private val binding: ListItemPhotoExampleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PhotoExample) {
            if (item.isGoodExample) {
                binding.goodExampleImage.setImageResource(item.image)
                binding.goodExampleImage.visible(true)
            } else {
                binding.badExampleImage.setImageResource(item.image)
                binding.badExampleImage.visible(true)
            }
            binding.exampleDesc.setContent {
                ExampleDescription(binding.root.context, item)
            }
        }
    }

    @Composable
    internal fun ExampleDescription(context: Context, item: PhotoExample) {
        Column {
            item.feedbackResId.forEach {
                Row(Modifier.padding(0.dp, 4.dp, 0.dp, 0.dp)) {
                    if (item.isGoodExample) {
                        Icon(
                            painter = painterResource(R.drawable.ic_checkmark_only),
                            contentDescription = null,
                            tint = Color(context.getColor(R.color.success))
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_error_only),
                            contentDescription = null,
                            tint = Color(context.getColor(R.color.error))
                        )
                    }
                    Text(
                        modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp),
                        text = context.getString(it),
                        fontSize = 14.sp,
                        color = Color(context.getColor(R.color.colorOnSurface)),
                    )
                }
            }
        }
    }
}

class PhotoExampleAdapterDiffCallback : DiffUtil.ItemCallback<PhotoExample>() {

    override fun areItemsTheSame(oldItem: PhotoExample, newItem: PhotoExample): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: PhotoExample, newItem: PhotoExample): Boolean {
        return oldItem.isGoodExample == newItem.isGoodExample &&
            oldItem.image == newItem.image &&
            oldItem.feedbackResId.size == newItem.feedbackResId.size
    }
}
