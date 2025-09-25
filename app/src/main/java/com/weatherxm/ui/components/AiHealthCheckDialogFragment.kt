package com.weatherxm.ui.components

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.weatherxm.R
import com.weatherxm.databinding.FragmentAiHealthCheckBinding
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.compose.MarkdownText
import com.weatherxm.ui.components.compose.Title
import com.weatherxm.ui.devicedetails.DeviceDetailsViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AiHealthCheckDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: FragmentAiHealthCheckBinding
    private val model: DeviceDetailsViewModel by activityViewModel()

    companion object {
        const val TAG = "AiHealthCheckDialogFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAiHealthCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.onHealthCheckData().observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                onData(it.data)
            } else if (it.status == Status.ERROR) {
                onError()
            }
        }
    }

    fun onError() {
        binding.loadingContainer.visible(false)
        binding.dataContainer.visible(false)
        binding.errorContainer.visible(true)
    }

    fun onData(data: String?) {
        binding.loadingContainer.visible(false)
        binding.errorContainer.visible(false)
        binding.dataContainer.setContent {
            AiHealthCheckData(data ?: "")
        }
        binding.dataContainer.visible(true)
    }

    fun show(fragment: Fragment) {
        show(fragment.childFragmentManager, TAG)
    }
}

@Suppress("FunctionNaming")
@Composable
@Preview
fun AiHealthCheckData(data: String = "lorem ipsum dolor sit amet") {
    Column(
        verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_large))
    ) {
        Title(stringResource(R.string.ai_health_check), 20.sp)
        MarkdownText(data)
    }
}
