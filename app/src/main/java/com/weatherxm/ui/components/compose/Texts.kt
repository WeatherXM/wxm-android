package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.weatherxm.R


@Suppress("FunctionNaming")
@Composable
fun SmallText(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    colorRes: Int = R.color.colorOnSurface,
    paddingValues: PaddingValues = PaddingValues()
) {
    Text(
        text = text,
        fontWeight = fontWeight,
        color = colorResource(colorRes),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(paddingValues)
    )
}

@Suppress("FunctionNaming")
@Composable
fun MediumText(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    colorRes: Int = R.color.colorOnSurface,
    paddingValues: PaddingValues = PaddingValues()
) {
    Text(
        text = text,
        fontWeight = fontWeight,
        color = colorResource(colorRes),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(paddingValues)
    )
}

@Suppress("FunctionNaming")
@Composable
fun LargeText(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: TextUnit = 16.sp,
    colorRes: Int = R.color.colorOnSurface
) {
    Text(
        text = text,
        fontWeight = fontWeight,
        fontSize = fontSize,
        color = colorResource(colorRes),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Suppress("FunctionNaming")
@Composable
fun Title(
    text: String,
    fontSize: TextUnit = 24.sp,
    colorRes: Int = R.color.colorOnSurface
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        color = colorResource(colorRes),
        style = MaterialTheme.typography.headlineSmall
    )
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun TextWithStartingIcon(
    text: String = "",
    textColorRes: Int = R.color.darkGrey,
    iconRes: Int = R.drawable.ic_one_filled,
    iconColorRes: Int = R.color.darkGrey
) {
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorResource(iconColorRes)
        )
        MediumText(
            text = text,
            colorRes = textColorRes
        )
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun MarkdownText(
    text: String = "",
    textColorRes: Int = R.color.colorOnSurface,
    linkColorRes: Int = R.color.colorPrimary
) {
    Markdown(
        content = text,
        colors = DefaultMarkdownColors(
            text = colorResource(textColorRes),
            codeBackground = Color.Transparent,
            inlineCodeBackground = Color.Transparent,
            dividerColor = Color.Transparent,
            tableBackground = Color.Transparent,
        ),
        typography = DefaultMarkdownTypography(
            h1 = MaterialTheme.typography.headlineLarge,
            h2 = MaterialTheme.typography.headlineMedium,
            h3 = MaterialTheme.typography.headlineSmall,
            h4 = MaterialTheme.typography.titleLarge,
            h5 = MaterialTheme.typography.titleMedium,
            h6 = MaterialTheme.typography.titleSmall,
            text = MaterialTheme.typography.bodyMedium,
            code = MaterialTheme.typography.bodyMedium,
            inlineCode = MaterialTheme.typography.bodyMedium,
            quote = MaterialTheme.typography.bodyMedium,
            paragraph = MaterialTheme.typography.bodyMedium,
            ordered = MaterialTheme.typography.bodyMedium,
            bullet = MaterialTheme.typography.bodyMedium,
            list = MaterialTheme.typography.bodyMedium,
            textLink = TextLinkStyles(
                style = SpanStyle(
                    color = colorResource(linkColorRes),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ),
            table = MaterialTheme.typography.bodyMedium
        )
    )
}

@Suppress("FunctionNaming")
@Composable
fun TextQuestWXMAllocated(amount: Int, amountFontSize: TextUnit) {
    LargeText(amount.toString(), FontWeight.Bold, amountFontSize)
    Text(
        text = "\$WXM",
        color = colorResource(R.color.darkGrey),
        style = MaterialTheme.typography.bodySmall,
        fontSize = 11.sp,
        modifier = Modifier.padding(
            top = when (amountFontSize) {
                14.sp -> 1.dp
                16.sp -> 2.dp
                else -> 4.dp
            }
        )
    )
}
