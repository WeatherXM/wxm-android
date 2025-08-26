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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import com.weatherxm.ui.components.compose.MediumText
import com.weatherxm.ui.components.compose.Title
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R

class GenericStepActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }
}

@Preview
@Composable
private fun Content() {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_normal))
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
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Title("Generic Step Activity", colorRes = R.color.textColor)
                MediumText("Generic Step Activity")
            }
        }
    }
}
