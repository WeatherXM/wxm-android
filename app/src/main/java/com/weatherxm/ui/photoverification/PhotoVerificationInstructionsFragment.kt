package com.weatherxm.ui.photoverification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.databinding.FragmentPhotoVerificationInstructionsBinding
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class PhotoVerificationInstructionsFragment : BaseFragment() {

    private lateinit var binding: FragmentPhotoVerificationInstructionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPhotoVerificationInstructionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.photoAmountInstructionsList.setContent {
            PhotoAmountInstructionsList()
        }
    }

    fun onIntroScreen(onClose: () -> Unit, onTakePhoto: () -> Unit) {
        binding.closeOrBackButton.setImageResource(R.drawable.ic_close)
        binding.closeOrBackButton.setOnClickListener {
            onClose()
        }

        with(binding.termsCheckboxDesc) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    navigator.openWebsite(activity, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(R.string.accept_terms, getString(R.string.terms_of_service_url))
        }

        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.takePhotoBtn.isEnabled = isChecked
        }

        binding.takePhotoBtn.setOnClickListener {
            onTakePhoto()
        }
    }

    fun onInstructionBottomSheet(onBack: () -> Unit) {
        binding.closeOrBackButton.setImageResource(R.drawable.ic_back)
        binding.closeOrBackButton.setOnClickListener {
            onBack()
        }
        binding.termsCheckbox.visible(false)
        binding.termsCheckboxDesc.visible(false)
        binding.takePhotoBtn.visible(false)
    }

    @Composable
    internal fun BulletWithText(text: String) {
        Row(Modifier.padding(6.dp, 2.dp, 0.dp, 0.dp)) {
            Text(
                text = "•",
                fontSize = 14.sp,
                color = Color(requireContext().getColor(R.color.colorOnSurface)),
            )
            Text(
                modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp),
                text = text,
                fontSize = 14.sp,
                color = Color(requireContext().getColor(R.color.colorOnSurface)),
            )
        }
    }

    @Composable
    internal fun PhotoAmountInstructionsList() {
        Column {
            BulletWithText(getString(R.string.photos_instruction_angles_1))
            BulletWithText(getString(R.string.photos_instruction_angles_2))
            BulletWithText(getString(R.string.photos_instruction_angles_3))
            BulletWithText(getString(R.string.photos_instruction_angles_4))
        }
    }
}
