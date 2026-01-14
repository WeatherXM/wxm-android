package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.sp
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun DowngradeDialog(shouldShow: Boolean, onDowngrade: () -> Unit, onClose: () -> Unit) {
    if (shouldShow) {
        AlertDialog(
            containerColor = colorResource(R.color.colorSurface),
            onDismissRequest = onClose,
            title = {
                Text(
                    text = stringResource(R.string.downgrade_to_free_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colorResource(R.color.darkestBlue)
                )
            },
            text = {
                Row(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.downgrade_to_free_dialog_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.colorOnSurface)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDowngrade,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium))
                ) {
                    Text(
                        text = stringResource(R.string.downgrade),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.colorPrimary),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.colorPrimary),
                    )
                ) {
                    Text(
                        text = stringResource(R.string.stay_on_premium),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.colorOnPrimary),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
