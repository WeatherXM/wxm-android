package com.weatherxm.ui.devicesettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weatherxm.R
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun DeviceInfoItemAlertView(alert: DeviceAlert, onRssiTroubleshoot: () -> Unit) {
    var backgroundResId = R.color.blueTint
    var drawable = R.drawable.ic_info
    var drawableTint = R.color.infoStrokeColor

    if (alert.severity == SeverityLevel.WARNING) {
        backgroundResId = R.color.warningTint
        drawable = R.drawable.ic_warning_hex_filled
        drawableTint = R.color.warning
    } else if (alert.severity == SeverityLevel.ERROR) {
        backgroundResId = R.color.errorTint
        drawable = R.drawable.ic_error_hex_filled
        drawableTint = R.color.error
    }

    var htmlMessage: Int? = null
    var message: Int? = null
    if (alert.alert == DeviceAlertType.LOW_BATTERY) {
        htmlMessage = R.string.battery_level_low_message
    } else if (alert.alert == DeviceAlertType.LOW_GATEWAY_BATTERY) {
        htmlMessage = R.string.gateway_battery_level_low_message
    } else if (alert.alert == DeviceAlertType.LOW_STATION_RSSI) {
        if (alert.severity == SeverityLevel.WARNING) {
            message = R.string.station_signal_low
        } else if (alert.severity == SeverityLevel.ERROR) {
            htmlMessage = R.string.station_signal_no_signal
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(backgroundResId)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_normal_to_large))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(dimensionResource(R.dimen.padding_small))
            ) {
                if (alert.alert != DeviceAlertType.LOW_BATTERY
                    && alert.alert != DeviceAlertType.LOW_GATEWAY_BATTERY
                ) {
                    Icon(
                        painter = painterResource(drawable),
                        tint = colorResource(drawableTint),
                        modifier = Modifier.size(20.dp),
                        contentDescription = null
                    )
                }
                Column(
                    verticalArrangement = spacedBy(dimensionResource(R.dimen.padding_extra_small))
                ) {
                    htmlMessage?.let {
                        Text(
                            text = AnnotatedString.fromHtml(htmlString = stringResource(id = it)),
                            color = colorResource(R.color.colorOnSurface),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    message?.let {
                        Text(
                            text = AnnotatedString(stringResource(it)),
                            color = colorResource(R.color.colorOnSurface),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (alert.alert == DeviceAlertType.LOW_STATION_RSSI) {
                        Row(
                            modifier = Modifier.clickable { onRssiTroubleshoot() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.troubleshoot_instructions_here),
                                color = colorResource(R.color.colorPrimary),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_open_new),
                                tint = colorResource(R.color.colorPrimary),
                                modifier = Modifier
                                    .padding(start = dimensionResource(R.dimen.padding_extra_small))
                                    .size(15.dp),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewDeviceInfoItemAlertView() {
    DeviceInfoItemAlertView(
        alert = DeviceAlert(
            alert = DeviceAlertType.LOW_STATION_RSSI,
            severity = SeverityLevel.WARNING
        )
    ) {}
}
