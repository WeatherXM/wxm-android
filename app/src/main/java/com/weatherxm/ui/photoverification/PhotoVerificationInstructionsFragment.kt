package com.weatherxm.ui.photoverification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.weatherxm.R
import com.weatherxm.databinding.FragmentPhotoVerificationInstructionsBinding
import com.weatherxm.ui.common.PhotoExample
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

        val goodExamples = listOf(
            PhotoExample(
                R.drawable.photo_good_example_1,
                listOf(
                    R.string.photo_good_example_1_1,
                    R.string.photo_good_example_1_2,
                    R.string.photo_good_example_1_3
                ),
                true
            ),
            PhotoExample(
                R.drawable.photo_good_example_2,
                listOf(
                    R.string.photo_good_example_2_1,
                    R.string.photo_good_example_2_2,
                    R.string.photo_good_example_2_3
                ),
                true
            ),
            PhotoExample(
                R.drawable.photo_good_example_3,
                listOf(
                    R.string.photo_good_example_3_1,
                    R.string.photo_good_example_3_2,
                    R.string.photo_good_example_3_3
                ),
                true
            ),
            PhotoExample(
                R.drawable.photo_good_example_4,
                listOf(R.string.photo_good_example_4_1),
                true
            )
        )
        val badExamples = listOf(
            PhotoExample(
                R.drawable.photo_bad_example_1,
                listOf(
                    R.string.photo_bad_example_1_1,
                    R.string.photo_bad_example_1_2,
                    R.string.photo_bad_example_1_3
                ),
                false
            ),
            PhotoExample(
                R.drawable.photo_bad_example_2,
                listOf(R.string.photo_bad_example_2_1, R.string.photo_bad_example_2_2),
                false
            ),
            PhotoExample(
                R.drawable.photo_bad_example_3,
                listOf(R.string.photo_bad_example_3_1, R.string.photo_bad_example_3_2),
                false
            ),
            PhotoExample(
                R.drawable.photo_bad_example_4,
                listOf(R.string.photo_bad_example_4_1, R.string.photo_bad_example_4_2),
                false
            )
        )

        val goodExampleAdapter = PhotoExampleAdapter()
        val badExampleAdapter = PhotoExampleAdapter()
        binding.goodExamplesRecycler.adapter = goodExampleAdapter
        binding.badExamplesRecycler.adapter = badExampleAdapter

        goodExampleAdapter.submitList(goodExamples)
        badExampleAdapter.submitList(badExamples)
    }

    fun onIntroScreen(onClose: () -> Unit, onTakePhoto: () -> Unit) {
        binding.closeOrBackButton.setImageResource(R.drawable.ic_close)
        binding.closeOrBackButton.setOnClickListener {
            onClose()
        }

        with(binding.acceptableUsePolicyCheckboxDesc) {
            movementMethod = BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    navigator.openWebsite(activity, url)
                    return@setOnLinkClickListener true
                }
            }
            setHtml(
                R.string.accept_acceptable_use_policy,
                getString(R.string.acceptable_use_policy_url)
            )
        }

        binding.acceptableUsePolicyCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.takePhotoBtn.isEnabled = isChecked
        }

        binding.takePhotoBtn.setOnClickListener {
            onTakePhoto()
        }
    }

    fun onInstructionScreen(onBack: () -> Unit) {
        binding.closeOrBackButton.setImageResource(R.drawable.ic_back)
        binding.closeOrBackButton.setOnClickListener {
            onBack()
        }
        binding.acceptableUsePolicyCheckbox.visible(false)
        binding.acceptableUsePolicyCheckboxDesc.visible(false)
        binding.takePhotoBtn.visible(false)
    }
}
