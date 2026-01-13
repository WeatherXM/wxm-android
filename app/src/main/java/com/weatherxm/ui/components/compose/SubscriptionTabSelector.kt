package com.weatherxm.ui.components.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weatherxm.R

/**
 * A reusable tab selector component with stateful tab management.
 *
 * @param defaultSelectedIndex Index of the initially selected tab (default: 0)
 * @param onTabSelected Callback invoked when a tab is selected, providing the index and label
 */
@Suppress("FunctionNaming", "MagicNumber")
@Composable
fun SubscriptionTabSelector(
    defaultSelectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(defaultSelectedIndex) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.margin_large),
                vertical = dimensionResource(R.dimen.margin_normal)
            )
            .height(50.dp),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_extra_extra_large)),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.colorSurface)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_normal)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_extra_small)),
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.padding_extra_small)
            )
        ) {
            TabItem(
                label = stringResource(R.string.monthly),
                isSelected = selectedIndex == 0,
                onClick = {
                    selectedIndex = 0
                    onTabSelected(0)
                }
            )
            TabItem(
                label = stringResource(R.string.annual),
                isSelected = selectedIndex == 1,
                onClick = {
                    selectedIndex = 1
                    onTabSelected(1)
                }
            )
        }
    }
}

@Suppress("FunctionNaming", "MagicNumber")
@Composable
private fun RowScope.TabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1F)
            .clickable { onClick() },
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_extra_extra_large)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                colorResource(R.color.blueTint)
            } else {
                Color.Transparent
            }
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) {
                    colorResource(R.color.textColor)
                } else {
                    colorResource(R.color.darkGrey)
                },
            )
        }
    }
}

@Suppress("FunctionNaming")
@Preview(showBackground = true)
@Composable
fun PreviewSubscriptionTabSelector() {
    SubscriptionTabSelector(defaultSelectedIndex = 1) { }
}
