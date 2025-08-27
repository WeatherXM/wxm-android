package com.weatherxm.ui.home.quests.steps

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.weatherxm.ui.components.BaseActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.dimensionResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.weatherxm.ui.components.compose.MediumText
import com.weatherxm.ui.components.compose.Title
import com.weatherxm.databinding.ActivityGenericStepBinding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R

class GenericStepActivity: BaseActivity() {

    private lateinit var binding: ActivityGenericStepBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenericStepBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.composeView.setContent {
            Content()
        }
    }
}

@Preview
@Composable
private fun Content() {
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
                            painter = painterResource(R.drawable.ic_calendar_solid),
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
                            "Generic Step Activity",
                            colorRes = R.color.textColor
                        )
                        MediumText("Generic Step Activity")
                    }
                }
            }
            CtaButton()

        }
    }
}

@Composable
private fun CtaButton() {
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
            stringResource(R.string.action_buy_station),
            fontWeight = FontWeight.Bold,
            colorRes = R.color.colorBackground
        )
    }
}
