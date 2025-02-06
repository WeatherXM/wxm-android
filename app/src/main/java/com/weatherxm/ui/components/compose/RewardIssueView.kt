package com.weatherxm.ui.components.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weatherxm.R
import com.weatherxm.data.models.SeverityLevel

@Suppress("FunctionNaming", "MagicNumber")
@Composable
fun RewardIssueView(
    title: String?,
    subtitle: String?,
    action: String?,
    severityLevel: SeverityLevel?,
    onClickListener: (() -> Unit)? = null
) {
    val (backgroundResId, strokeResId) = when (severityLevel) {
        SeverityLevel.INFO -> Pair(R.color.blueTint, R.color.infoStrokeColor)
        SeverityLevel.WARNING -> Pair(R.color.warningTint, R.color.warning)
        SeverityLevel.ERROR -> Pair(R.color.errorTint, R.color.error)
        else -> Pair(R.color.blueTint, R.color.infoStrokeColor)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(backgroundResId)),
        elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.elevation_normal)),
        border = BorderStroke(1.dp, colorResource(strokeResId)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.padding_large))) {
            title?.let {
                Text(
                    text = it,
                    color = colorResource(R.color.colorOnSurface),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            subtitle?.let {
                MediumText(
                    it,
                    paddingValues = PaddingValues(top = dimensionResource(R.dimen.padding_small))
                )
            }
            if (action != null && onClickListener != null) {
                Button(
                    onClick = { onClickListener() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.translucent),
                        contentColor = colorResource(R.color.colorPrimary)
                    ),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.padding_normal))
                ) {
                    MediumText(
                        action,
                        fontWeight = FontWeight.Bold,
                        colorRes = R.color.colorPrimary
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewRewardIssueView() {
    RewardIssueView(
        title = "Issue title is here.",
        subtitle = "Issue message is here.",
        action = "Action",
        severityLevel = SeverityLevel.WARNING,
        onClickListener = {}
    )
}
