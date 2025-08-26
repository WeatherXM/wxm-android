package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun QuestsPageSelector(selectedPage: State<Int>, onSelectPage: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(50.dp),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_extra_extra_large)),
        elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.elevation_normal))
    ) {
        Row {
            FilterChip(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1F),
                selected = selectedPage.value == 0,
                shape = RoundedCornerShape(bottomEnd = 0.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colorResource(R.color.layer2),
                    labelColor = colorResource(R.color.darkGrey),
                    selectedContainerColor = colorResource(R.color.colorSurface),
                    selectedLabelColor = colorResource(R.color.colorPrimary)
                ),
                onClick = { onSelectPage(0) },
                border = null,
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.ic_nav_quests),
                            contentDescription = "Quests"
                        )
                    }
                }
            )
            FilterChip(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1F),
                selected = selectedPage.value == 1,
                onClick = { onSelectPage(1) },
                shape = RoundedCornerShape(bottomEnd = 0.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colorResource(R.color.layer2),
                    labelColor = colorResource(R.color.darkGrey),
                    selectedContainerColor = colorResource(R.color.colorSurface),
                    selectedLabelColor = colorResource(R.color.colorPrimary)
                ),
                border = null,
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.ic_checkmark),
                            contentDescription = "Completed Quests",
                        )
                    }
                }
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
@Preview
fun QuestsPageTogglePreview() {
    QuestsPageSelector(selectedPage = remember { mutableIntStateOf(0) }) {}
}
