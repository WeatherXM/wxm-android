package com.weatherxm.ui.components.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.weatherxm.ui.common.ActionForMessageView
import com.weatherxm.ui.common.DataForMessageView
import com.weatherxm.ui.common.SubtitleForMessageView

@Suppress("FunctionNaming", "LongMethod", "MagicNumber", "CyclomaticComplexMethod")
@Composable
fun MessageCardView(data: DataForMessageView) {
    var (backgroundResId, strokeAndIconColor) = when (data.severityLevel) {
        SeverityLevel.INFO -> Pair(R.color.blueTint, R.color.infoStrokeColor)
        SeverityLevel.WARNING -> Pair(R.color.warningTint, R.color.warning)
        SeverityLevel.ERROR -> Pair(R.color.errorTint, R.color.error)
    }
    val border = if (data.useStroke) {
        BorderStroke(1.dp, colorResource(strokeAndIconColor))
    } else {
        null
    }
    val paddingNormal = dimensionResource(R.dimen.padding_normal)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorResource(backgroundResId)),
        border = border,
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
    ) {
        Column(
            modifier = Modifier
                .padding(paddingNormal)
                .padding(top = data.extraTopPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(dimensionResource(R.dimen.padding_small))
            ) {
                data.drawable?.let {
                    Icon(
                        painter = painterResource(it),
                        tint = colorResource(data.drawableTint ?: strokeAndIconColor),
                        modifier = Modifier.size(20.dp),
                        contentDescription = null
                    )
                }
                Column(verticalArrangement = spacedBy(dimensionResource(R.dimen.padding_small))) {
                    if (data.title != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(data.title),
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.colorOnSurface),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(if (data.onCloseListener != null) 0.8F else 1F)
                            )
                            if (data.onCloseListener != null) {
                                IconButton(
                                    onClick = { data.onCloseListener.invoke() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = stringResource(R.string.action_close),
                                        tint = colorResource(R.color.colorOnSurface)
                                    )
                                }
                            }
                        }
                    }
                    data.subtitle?.message?.let {
                        MediumText(stringResource(it))
                    }
                    data.subtitle?.messageAsString?.let {
                        MediumText(it)
                    }
                    data.subtitle?.htmlMessage?.let {
                        Text(
                            color = colorResource(R.color.colorOnSurface),
                            style = MaterialTheme.typography.bodyMedium,
                            text = AnnotatedString.fromHtml(htmlString = stringResource(id = it))
                        )
                    }
                    data.subtitle?.htmlMessageAsString?.let {
                        Text(
                            color = colorResource(R.color.colorOnSurface),
                            style = MaterialTheme.typography.bodyMedium,
                            text = AnnotatedString.fromHtml(htmlString = it)
                        )
                    }
                }
            }
            if (data.action != null) {
                Button(
                    onClick = { data.action.onClickListener() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(data.action.backgroundTint),
                        contentColor = colorResource(data.action.foregroundTint)
                    ),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.padding_small_to_normal))
                ) {
                    if (data.action.endIcon != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(modifier = Modifier.width(24.dp))
                            MediumText(
                                stringResource(data.action.label),
                                fontWeight = FontWeight.Bold,
                                colorRes = data.action.foregroundTint
                            )
                            Icon(
                                painter = painterResource(data.action.endIcon),
                                contentDescription = null,
                                tint = colorResource(data.action.foregroundTint),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        data.action.startIcon?.let {
                            Icon(
                                painter = painterResource(it),
                                contentDescription = null,
                                tint = colorResource(data.action.foregroundTint),
                                modifier = Modifier.padding(
                                    end = dimensionResource(R.dimen.padding_small)
                                )
                            )
                        }
                        MediumText(
                            stringResource(data.action.label),
                            fontWeight = FontWeight.Bold,
                            colorRes = data.action.foregroundTint
                        )
                    }
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewMessageCardView() {
    MessageCardView(
        data = DataForMessageView(
            extraTopPadding = 0.dp,
            title = R.string.preview_message_title,
            subtitle = SubtitleForMessageView(message = R.string.preview_message_subtitle),
            drawable = R.drawable.ic_warning_hex_filled,
            action = ActionForMessageView(
                label = R.string.action_ok,
                backgroundTint = R.color.colorPrimary,
                foregroundTint = R.color.colorOnPrimary,
                startIcon = null,
                onClickListener = {}
            ),
            useStroke = false,
            severityLevel = SeverityLevel.WARNING,
            onCloseListener = {}
        )
    )
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewMessageCardViewWithTopPadding() {
    MessageCardView(
        data = DataForMessageView(
            extraTopPadding = 16.dp,
            title = R.string.preview_message_title,
            subtitle = SubtitleForMessageView(message = R.string.preview_message_subtitle),
            drawable = R.drawable.ic_warning_hex_filled,
            action = ActionForMessageView(
                label = R.string.action_ok,
                backgroundTint = R.color.colorPrimary,
                foregroundTint = R.color.colorOnPrimary,
                startIcon = null,
                endIcon = R.drawable.ic_open_new,
                onClickListener = {}
            ),
            useStroke = false,
            severityLevel = SeverityLevel.WARNING,
            onCloseListener = null
        )
    )
}
