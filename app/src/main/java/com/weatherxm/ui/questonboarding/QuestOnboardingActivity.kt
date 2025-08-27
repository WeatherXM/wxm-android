package com.weatherxm.ui.questonboarding

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.dimensionResource
import com.weatherxm.R
import com.weatherxm.databinding.ActivityQuestOnboardingBinding
import com.weatherxm.ui.common.Contracts.ARG_USER_ID
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.compose.HeaderView
import com.weatherxm.ui.components.compose.QuestStepDetailed
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class QuestOnboardingActivity : BaseActivity() {
    private lateinit var binding: ActivityQuestOnboardingBinding
    private val model: QuestOnboardingViewModel by viewModel {
        parametersOf(intent.getStringExtra(ARG_USER_ID))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        binding.header.setContent {
            HeaderView(
                title = getString(R.string.onboarding_quest),
                subtitle = getString(R.string.onboarding_quest_subtitle),
                null
            )
        }

        model.onError().observe(this) {
            if (it != null) {
                binding.loading.visible(false)
                showSnackbarMessage(
                    binding.root,
                    it.message ?: getString(R.string.error_generic_message),
                    { model.getData() },
                    R.string.action_retry,
                    null
                )
            }
        }

        model.onDataLoaded().observe(this) {
            binding.loading.visible(false)
            model.onboardingQuestData?.let { data ->
                binding.header.setContent {
                    HeaderView(
                        title = data.title,
                        subtitle = data.description,
                        null
                    )
                }
                binding.steps.setContent {
                    Column(
                        verticalArrangement = spacedBy(dimensionResource(R.dimen.padding_large))
                    ) {
                        data.steps.forEach {
                            QuestStepDetailed(it) {
                                if (!data.isCompleted && !it.isCompleted) {
                                    // TODO: Handle this click for each step
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.loading.visible(true)
        model.getData()
    }
}
