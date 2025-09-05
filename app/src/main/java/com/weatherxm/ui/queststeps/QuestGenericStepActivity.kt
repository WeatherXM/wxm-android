package com.weatherxm.ui.queststeps

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.weatherxm.R
import com.weatherxm.databinding.ActivityQuestGenericStepBinding
import com.weatherxm.ui.common.Contracts.ARG_QUEST_STEP
import com.weatherxm.ui.common.Contracts.ARG_USER_ID
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.ui.common.QuestStepType
import com.weatherxm.ui.common.ctaButtonTitle
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.stepIcon
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.compose.MediumText
import com.weatherxm.ui.components.compose.Title
import com.weatherxm.util.hasPermission
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class QuestGenericStepActivity : BaseActivity() {
    private val model: QuestGenericStepViewModel by viewModel() {
        parametersOf(
            intent.parcelable<QuestStep>(ARG_QUEST_STEP),
            intent.getStringExtra(ARG_USER_ID) ?: String.empty()
        )
    }

    private lateinit var binding: ActivityQuestGenericStepBinding
    private var ctaButtonTapped = false
    private val senderForSolanaWallet = ActivityResultSender(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestGenericStepBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        model.onStepCompleted().observe(this) {
            binding.loading.visible(false)
            handleStepRequestCompletion(it) {
                handleCtaClick()
            }
        }

        model.onStepSkipped().observe(this) {
            binding.loading.visible(false)
            handleStepRequestCompletion(it) {
                handleSkipClick()
            }
        }

        model.onWalletAddressError().observe(this) { errorMsg ->
            binding.loading.visible(false)
            showSnackbarMessage(binding.root, errorMsg ?: getString(R.string.error_generic_message))
        }

        binding.composeView.setContent {
            Content(
                model.questStep,
                onCtaClick = { handleCtaClick() },
                onSkipClick = { handleSkipClick() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateStepState()
    }

    private fun handleCtaClick() {
        ctaButtonTapped = true
        when (model.questStep.type) {
            QuestStepType.CONNECT_WALLET -> {
                binding.loading.visible(true)
                model.connectSolanaWallet(senderForSolanaWallet)
            }
            QuestStepType.ENABLE_LOCATION_PERMISSION -> {
                requestLocationPermissions(this) {
                    updateStepState()
                }
            }
            QuestStepType.ENABLE_NOTIFICATIONS -> {
                // Request notification permission
                if (!hasPermission(POST_NOTIFICATIONS)) {
                    navigator.openAppSettings(this)
                } else {
                    updateStepState()
                }
            }
            QuestStepType.ENABLE_ENVIRONMENT_SENSORS -> {
                updateStepState()
            }
            QuestStepType.SOCIAL_FOLLOW_X -> {
                navigator.openWebsiteExternally(this, getString(R.string.x_url))
            }
            QuestStepType.UNKNOWN -> {
                // No action
            }
        }
    }

    private fun handleSkipClick() {
        binding.loading.visible(true)
        model.markStepAsSkipped()
    }

    private fun updateStepState() {
        if (!ctaButtonTapped) {
            return
        }
        when (model.questStep.type) {
            QuestStepType.CONNECT_WALLET -> {
                // Do nothing. We handle it differently via the ViewModel.
            }
            QuestStepType.ENABLE_LOCATION_PERMISSION -> {
                /**
                 * In case user taps the CTA, doesn't give permissions, backgrounds the app and
                 * opens it again (so onResume calls updateStepState). We need to make this check
                 * before proceeding with marking the step as completed.
                 */
                if (hasPermission(ACCESS_FINE_LOCATION) || hasPermission(ACCESS_COARSE_LOCATION)) {
                    binding.loading.visible(true)
                    model.markStepAsCompleted()
                }
            }
            QuestStepType.ENABLE_NOTIFICATIONS -> {
                if (hasPermission(POST_NOTIFICATIONS)) {
                    binding.loading.visible(true)
                    model.markStepAsCompleted()
                }
            }
            QuestStepType.ENABLE_ENVIRONMENT_SENSORS -> {
                showSensorsDialog()
            }
            QuestStepType.SOCIAL_FOLLOW_X -> {
                binding.loading.visible(true)
                model.markStepAsCompleted()
            }
            QuestStepType.UNKNOWN -> {
                // No action
            }
        }
    }

    private fun handleStepRequestCompletion(error: Throwable?, callback: () -> Unit) {
        if (error == null) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            showSnackbarMessage(
                binding.root,
                error.message ?: getString(R.string.error_generic_message),
                callback
            )
        }

        ctaButtonTapped = false
    }

    private fun showSensorsDialog() {
        ActionDialogFragment
            .Builder(
                title = getString(R.string.allow_sensors_dialog_title),
                message = getString(R.string.allow_sensors_dialog_message),
                negative = getString(R.string.do_not_allow_button_message)
            )
            .onPositiveClick(getString(R.string.allow_button_message)) {
                binding.loading.visible(true)
                model.markStepAsCompleted()
            }
            .build()
            .show(this)
    }
}

@Suppress("FunctionNaming")
@Composable
private fun Content(step: QuestStep, onCtaClick: () -> Unit, onSkipClick: () -> Unit) {
    Column(
        modifier = Modifier
            .background(color = colorResource(R.color.colorBackground))
            .padding(dimensionResource(R.dimen.padding_normal_to_large)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .background(
                        color = colorResource(R.color.colorSurface),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(step.type.stepIcon()),
                    contentDescription = null,
                    tint = colorResource(R.color.colorPrimary)
                )
            }

            Column(
                modifier = Modifier.padding(
                    vertical = dimensionResource(R.dimen.padding_normal_to_large)
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Title(text = step.title, textAlign = TextAlign.Center)

                MediumText(
                    text = step.description,
                    textAlign = TextAlign.Center,
                    colorRes = R.color.darkGrey,
                    paddingValues = PaddingValues(
                        top = dimensionResource(R.dimen.padding_small_to_normal)
                    )
                )
            }

            if (step.isOptional) {
                Box(
                    modifier = Modifier.background(
                        color = colorResource(R.color.colorSurface),
                        shape = CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    MediumText(
                        text = stringResource(R.string.optional_step_description),
                        paddingValues = PaddingValues(
                            start = dimensionResource(R.dimen.padding_normal_to_large),
                            end = dimensionResource(R.dimen.padding_normal_to_large),
                            top = dimensionResource(R.dimen.padding_small_to_normal),
                            bottom = dimensionResource(R.dimen.padding_small_to_normal)
                        )
                    )
                }
            }
        }
        CtaButtons(step, onCtaClick, onSkipClick)
    }
}

@Suppress("FunctionNaming")
@Composable
private fun CtaButtons(step: QuestStep, onCtaClick: () -> Unit, onSkipClick: () -> Unit) {
    Button(
        onClick = onCtaClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.colorPrimary),
            contentColor = colorResource(R.color.colorBackground)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
    ) {
        MediumText(
            stringResource(step.type.ctaButtonTitle()),
            fontWeight = FontWeight.Bold,
            colorRes = R.color.colorOnPrimary
        )
    }

    if (step.isOptional) {
        TextButton(
            onClick = onSkipClick
        ) {
            MediumText(
                stringResource(R.string.skip_and_mark_as_done_button_title),
                fontWeight = FontWeight.Bold,
                colorRes = R.color.colorPrimary
            )
        }
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewContent() {
    Content(
        step = QuestStep(
            "stepId",
            "Enable Environment Sensors",
            "Step Description Step Description Step Description Step Description",
            0,
            false,
            false,
            false,
            QuestStepType.CONNECT_WALLET
        ),
        onCtaClick = {},
        onSkipClick = {}
    )
}
