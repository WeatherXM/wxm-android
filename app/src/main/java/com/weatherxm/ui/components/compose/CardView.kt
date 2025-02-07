package com.weatherxm.ui.components.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun CardViewClickable(
    radiusResource: Int = R.dimen.radius_large,
    elevationResource: Int = R.dimen.elevation_normal,
    borderStroke: BorderStroke? = null,
    onClickListener: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.colorSurface)
        ),
        border = borderStroke,
        onClick = { onClickListener() },
        shape = RoundedCornerShape(dimensionResource(radiusResource)),
        elevation = CardDefaults.cardElevation(dimensionResource(elevationResource))
    ) {
        content()
    }
}
