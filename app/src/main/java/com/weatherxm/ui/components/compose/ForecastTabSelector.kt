package com.weatherxm.ui.components.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
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
fun ForecastTabSelector(
    defaultSelectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(defaultSelectedIndex) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
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
                includeIcon = false,
                label = stringResource(R.string.basic_forecast),
                isSelected = selectedIndex == 0,
                onClick = {
                    selectedIndex = 0
                    onTabSelected(0)
                }
            )
            TabItem(
                includeIcon = true,
                label = stringResource(R.string.hyperlocal),
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
    includeIcon: Boolean,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxHeight()
            .weight(1F),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = if (isSelected) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF8C97F5),
                                Color(0xFF7985E5)
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small))
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.margin_small)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (includeIcon) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_sparkles),
                        contentDescription = null,
                        tint = colorResource(R.color.dark_text)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        colorResource(R.color.dark_text)
                    } else {
                        colorResource(R.color.darkGrey)
                    },
                )

            }
        }
    }
}

@Suppress("FunctionNaming")
@Preview(showBackground = true)
@Composable
fun PreviewForecastTabSelector() {
    ForecastTabSelector(defaultSelectedIndex = 1) { }
}
