package com.weatherxm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.ui.common.UploadPhotosState

@Suppress("FunctionNaming")
@Composable
fun PhotoUploadState(state: UploadPhotosState, showStationName: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_extra_small))
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            if (state.error == null) {
                Text(
                    text = "${state.progress}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.colorOnSurface)
                )
                if (state.isSuccess) {
                    Text(
                        text = stringResource(R.string.completed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.colorOnSurface)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.uploading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.colorOnSurface)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.upload_failed_retry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorResource(R.color.colorOnSurface)
                )
            }
        }
        if (state.error == null) {
            LinearProgressIndicator(
                progress = { state.progress.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                strokeCap = StrokeCap.Round,
                color = colorResource(R.color.colorPrimary),
                trackColor = colorResource(R.color.layer2)
            )
        }
        if (showStationName) {
            Text(
                text = state.device.name,
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.darkGrey)
            )
        }
    }
}
