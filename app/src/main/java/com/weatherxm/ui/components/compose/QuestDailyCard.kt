package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
@Preview
fun QuestDailyCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.layer2)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
        elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.elevation_small))
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_normal)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_polygon),
                    contentDescription = null,
                    tint = colorResource(R.color.midGrey)
                )
                Icon(
                    modifier = Modifier.size(33.dp),
                    painter = painterResource(R.drawable.ic_calendar_solid),
                    contentDescription = null,
                    tint = colorResource(R.color.darkGrey)
                )
            }
            Column(
                modifier = Modifier.padding(start = dimensionResource(R.dimen.padding_normal))
            ) {
                Title(
                    text = stringResource(R.string.daily_quest),
                    fontSize = 20.sp
                )
                SmallText(
                    text = stringResource(R.string.coming_soon),
                    colorRes = R.color.darkGrey,
                    paddingValues = PaddingValues(top = dimensionResource(R.dimen.padding_small))
                )
            }
        }
    }
}
