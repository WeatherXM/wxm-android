package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun TermsDialog(shouldShow: Boolean, onUnderstand: () -> Unit) {
    if (shouldShow) {
        AlertDialog(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_normal)),
            containerColor = colorResource(R.color.colorSurface),
            onDismissRequest = { },
            title = {
                Text(
                    text = stringResource(R.string.terms_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colorResource(R.color.darkestBlue)
                )
            },
            text = {
                Row(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = AnnotatedString.fromHtml(
                            htmlString = stringResource(
                                R.string.terms_dialog_message,
                                stringResource(R.string.terms_of_use_owners_url),
                                stringResource(R.string.privacy_policy_owners_url)
                            ),
                            linkStyles = TextLinkStyles(
                                style = SpanStyle(
                                    color = colorResource(R.color.colorPrimary),
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorResource(R.color.colorOnSurface)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onUnderstand() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.colorPrimary),
                    )
                ) {
                    Text(
                        text = stringResource(R.string.action_i_understand),
                        style = MaterialTheme.typography.labelLarge,
                        color = colorResource(R.color.colorOnPrimary),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        )
    }
}
