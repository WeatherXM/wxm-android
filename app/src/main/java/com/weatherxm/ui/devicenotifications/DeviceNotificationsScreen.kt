package com.weatherxm.ui.devicenotifications

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.weatherxm.R
import com.weatherxm.ui.components.compose.MediumText

@Suppress("FunctionNaming")
@Preview
@Composable
fun DeviceNotificationsScreen(
    selectedChipLabelResId: Int = R.string.seven_days_abbr,
    enabledToggle: Boolean = true,
    onSelectedChanged: (Int) -> Unit = {},
) {
    val sevenDaysAbbrResId = R.string.seven_days_abbr
    val oneMonthAbbrResId = R.string.one_month_abbr
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.layer1)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small))
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_extra_small)),
            horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_extra_small))
        ) {
            Chip(
                labelResId = sevenDaysAbbrResId,
                isSelected = sevenDaysAbbrResId == selectedChipLabelResId,
                enabledToggle = enabledToggle,
                onSelectionChanged = { onSelectedChanged(it) },
            )
            Chip(
                labelResId = oneMonthAbbrResId,
                isSelected = oneMonthAbbrResId == selectedChipLabelResId,
                enabledToggle = enabledToggle,
                onSelectionChanged = { onSelectedChanged(it) },
            )
        }
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun Chip(
    labelResId: Int = R.string.seven_days_abbr,
    isSelected: Boolean = false,
    enabledToggle: Boolean = true,
    onSelectionChanged: (Int) -> Unit = {},
) {
    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small)),
        color = if (isSelected) colorResource(R.color.layer2) else colorResource(R.color.layer1)
    ) {
        Row(modifier = Modifier
            .padding(
                dimensionResource(R.dimen.padding_normal),
                dimensionResource(R.dimen.padding_small_to_normal)
            )
            .toggleable(
                value = isSelected,
                enabled = enabledToggle,
                onValueChange = {
                    onSelectionChanged(labelResId)
                }
            )
        ) {
            MediumText(
                text = stringResource(labelResId),
                colorRes = R.color.darkGrey,
            )
        }
    }
}
