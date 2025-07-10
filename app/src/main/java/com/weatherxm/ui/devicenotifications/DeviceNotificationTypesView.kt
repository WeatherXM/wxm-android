package com.weatherxm.ui.devicenotifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.data.models.DeviceNotificationType
import com.weatherxm.ui.components.compose.LargeText
import com.weatherxm.ui.components.compose.MediumText
import com.weatherxm.ui.components.compose.SwitchWithIcon

@Suppress("FunctionNaming")
@Composable
fun DeviceNotificationTypesView(
    isMainEnabled: Boolean,
    supportsFirmwareUpdate: Boolean,
    notificationTypesEnabled: SnapshotStateSet<DeviceNotificationType>,
    onNotificationTypeChanged: (DeviceNotificationType, Boolean) -> Unit
) {
    Column(verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))) {
        NotificationType(
            title = R.string.activity,
            subtitle = R.string.activity_notification_subtitle,
            isEnabled = isMainEnabled,
            isChecked = notificationTypesEnabled.contains(DeviceNotificationType.ACTIVITY),
            onCheckedChange = { onNotificationTypeChanged(DeviceNotificationType.ACTIVITY, it) }
        )
        NotificationType(
            title = R.string.battery,
            subtitle = R.string.battery_notification_subtitle,
            isEnabled = isMainEnabled,
            isChecked = notificationTypesEnabled.contains(DeviceNotificationType.BATTERY),
            onCheckedChange = { onNotificationTypeChanged(DeviceNotificationType.BATTERY, it) }
        )
        if (supportsFirmwareUpdate) {
            NotificationType(
                title = R.string.firmware_update,
                subtitle = R.string.firmware_update_notification_subtitle,
                isEnabled = isMainEnabled,
                isChecked = notificationTypesEnabled.contains(DeviceNotificationType.FIRMWARE),
                onCheckedChange = { onNotificationTypeChanged(DeviceNotificationType.FIRMWARE, it) }
            )
        }
        NotificationType(
            title = R.string.health,
            subtitle = R.string.health_notification_subtitle,
            isEnabled = isMainEnabled,
            isChecked = notificationTypesEnabled.contains(DeviceNotificationType.HEALTH),
            onCheckedChange = { onNotificationTypeChanged(DeviceNotificationType.HEALTH, it) }
        )
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
private fun NotificationType(
    title: Int = R.string.activity,
    subtitle: Int = R.string.activity_notification_subtitle,
    isEnabled: Boolean = true,
    isChecked: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1F)
                .padding(end = 6.dp),
            verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_extra_small))
        ) {
            LargeText(
                text = stringResource(title),
                fontSize = 20.sp
            )
            MediumText(
                text = stringResource(subtitle),
                colorRes = R.color.darkGrey,
            )
        }
        SwitchWithIcon(
            modifier = Modifier.padding(start = 6.dp),
            isEnabled = isEnabled,
            isChecked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }
}
