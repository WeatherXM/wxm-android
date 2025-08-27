package com.weatherxm.ui.home.quests.steps

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.weatherxm.ui.components.BaseActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.res.dimensionResource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.weatherxm.ui.components.compose.MediumText
import com.weatherxm.ui.components.compose.Title
import com.weatherxm.databinding.ActivityGenericStepBinding
import androidx.compose.ui.unit.dp
import com.weatherxm.R
import com.weatherxm.ui.common.Contracts.ARG_QUEST_STEP
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.ui.common.QuestStepType
import com.weatherxm.ui.common.ctaButtonTitle
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.stepIcon
import com.weatherxm.ui.components.compose.LargeText
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.getValue

class GenericStepActivity: BaseActivity() {
    private val model: GenericStepViewModel by viewModel() {
        parametersOf(intent.parcelable<QuestStep>(ARG_QUEST_STEP))
    }

    private lateinit var binding: ActivityGenericStepBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenericStepBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.composeView.setContent {
            Content(model.questStep)
        }
    }
}

@Composable
private fun Content(step: QuestStep) {
    Box(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.margin_large))
            .padding(bottom = dimensionResource(R.dimen.margin_large))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(
                            R.dimen.margin_normal
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .background(
                                color = colorResource(R.color.colorSurface),
                                shape = androidx.compose.foundation.shape.CircleShape
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
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(
                                R.dimen.margin_extra_small
                            )
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Title(
                            step.title,
                            colorRes = R.color.textColor
                        )
                        MediumText(step.description)
                    }

                    if (step.isOptional) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = colorResource(R.color.colorSurface),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            MediumText(
                                stringResource(R.string.optional_step_description),
                                paddingValues = PaddingValues(
                                    start = dimensionResource(R.dimen.padding_normal_to_large),
                                    end = dimensionResource(R.dimen.padding_normal_to_large),
                                    top = dimensionResource(R.dimen.padding_small),
                                    bottom = dimensionResource(
                                        R.dimen.padding_small
                                    )
                                )
                            )
                        }
                    }
                }
            }
            CtaButton(step)
        }
    }
}

@Composable
private fun CtaButton(step: QuestStep) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { },
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
                colorRes = R.color.colorBackground
            )
        }

        if (step.isOptional) {
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            ) {
                MediumText(
                    stringResource(R.string.skip_and_mark_as_done_button_title),
                    fontWeight = FontWeight.Bold,
                    colorRes = R.color.colorPrimary
                )
            }
        }
    }
}


@Preview
@Composable
private  fun PreviewContent() {
    Content(
        QuestStep("stepId",
            "Step Title",
            "Step Description",
            0,
            false,
            false,
            false,
            QuestStepType.CONNECT_WALLET)
    )
}
