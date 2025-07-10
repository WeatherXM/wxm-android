package com.weatherxm.ui.components.compose

import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun SwitchWithIcon(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isChecked: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Switch(
        modifier = modifier,
        enabled = isEnabled,
        checked = isChecked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            if (isChecked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_only),
                    contentDescription = null
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_error_only),
                    contentDescription = null
                )
            }
        },
        colors = SwitchColors(
            checkedThumbColor = colorResource(R.color.colorSurface),
            checkedTrackColor = colorResource(R.color.colorPrimary),
            uncheckedThumbColor = colorResource(R.color.darkGrey),
            uncheckedTrackColor = colorResource(R.color.layer2),
            checkedBorderColor = colorResource(R.color.colorPrimary),
            checkedIconColor = colorResource(R.color.darkestBlue),
            uncheckedBorderColor = colorResource(R.color.darkGrey),
            uncheckedIconColor = colorResource(R.color.colorSurface),
            disabledCheckedThumbColor = colorResource(R.color.colorSurface),
            disabledCheckedTrackColor = colorResource(R.color.midGrey),
            disabledCheckedBorderColor = colorResource(R.color.midGrey),
            disabledCheckedIconColor = colorResource(R.color.darkGrey),
            disabledUncheckedThumbColor = colorResource(R.color.midGrey),
            disabledUncheckedTrackColor = colorResource(R.color.layer2),
            disabledUncheckedBorderColor = colorResource(R.color.midGrey),
            disabledUncheckedIconColor = colorResource(R.color.colorSurface)
        )
    )
}
