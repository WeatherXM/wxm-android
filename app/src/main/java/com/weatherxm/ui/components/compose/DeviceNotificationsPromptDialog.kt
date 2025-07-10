package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun DeviceNotificationsPromptDialog(
    shouldShow: Boolean,
    onDismiss: () -> Unit,
    onTakeMeThere: () -> Unit
) {
    if (shouldShow) {
        AlertDialog(
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.padding_normal_to_large)
            ),
            containerColor = colorResource(R.color.colorSurface),
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.station_notifications),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colorResource(R.color.darkestBlue)
                )
            },
            text = {
                Row(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.station_notifications_dialog_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.colorOnSurface)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))) {
                    TextButton(
                        modifier = Modifier.weight(1F),
                        onClick = onDismiss,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium))
                    ) {
                        Text(
                            text = stringResource(R.string.action_maybe_later),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorResource(R.color.colorPrimary),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        modifier = Modifier.weight(1F),
                        onClick = onTakeMeThere,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.colorPrimary),
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_take_me_there),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorResource(R.color.colorOnPrimary),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        )
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun DeviceNotificationsPromptDialogPreview() {
    DeviceNotificationsPromptDialog(
        shouldShow = true,
        onDismiss = {},
        onTakeMeThere = {}
    )
}
