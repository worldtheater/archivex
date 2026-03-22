package com.worldtheater.archive.feature.note_list.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.security.*
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.AppDialogSecondaryButton
import com.worldtheater.archive.ui.widget.AppDialogTitle

private data class GeneratorPasswordStrengthUi(
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrongPasswordGeneratorDialog(
    titleText: String,
    passwordLabel: String,
    copyPasswordContentDescription: String,
    regeneratePasswordText: String,
    passwordLengthFormatter: @Composable (Int) -> String,
    passwordStrengthFormatter: @Composable (PasswordStrengthLevel) -> String,
    onDismiss: () -> Unit,
    onCopyPassword: (label: String, password: String) -> Unit
) {
    var passwordLength by remember { mutableIntStateOf(DEFAULT_GENERATED_PASSWORD_LENGTH) }
    var generatedPassword by remember {
        mutableStateOf(generateStrongPassword(passwordLength))
    }
    val strengthResult = remember(generatedPassword) {
        evaluateGeneratedPasswordStrength(generatedPassword)
    }
    val strengthUi = strengthUi(
        level = strengthResult.level,
        error = MaterialTheme.colorScheme.error,
        ok = MaterialTheme.colorScheme.primary
    )

    AppDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = SmoothRoundedCornerShape(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)
            ) {
                AppDialogTitle(
                    text = titleText,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(18.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SmoothRoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = generatedPassword,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val copyButtonShape = SmoothRoundedCornerShape(12.dp)
                        Surface(
                            shape = copyButtonShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clip(copyButtonShape)
                                .clickable {
                                    onCopyPassword(passwordLabel, generatedPassword)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = copyPasswordContentDescription,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(34.dp)
                                    .padding(9.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(18.dp))

                Slider(
                    value = passwordLength.toFloat(),
                    onValueChange = { raw ->
                        val newLength = raw.toInt().coerceIn(
                            MIN_GENERATED_PASSWORD_LENGTH,
                            MAX_GENERATED_PASSWORD_LENGTH
                        )
                        if (newLength != passwordLength) {
                            passwordLength = newLength
                            generatedPassword = generateStrongPassword(newLength)
                        }
                    },
                    valueRange = MIN_GENERATED_PASSWORD_LENGTH.toFloat()..MAX_GENERATED_PASSWORD_LENGTH.toFloat(),
                    steps = 0,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = appOnSurfaceA10(),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = appOnSurfaceA10(),
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            drawStopIndicator = null,
                            trackInsideCornerSize = 0.dp,
                            modifier = Modifier.height(3.dp)
                        )
                    },
                    modifier = Modifier.height(8.dp)
                )

                Spacer(modifier = Modifier.size(12.dp))

                Text(
                    text = passwordLengthFormatter(passwordLength),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.size(6.dp))

                Text(
                    text = passwordStrengthFormatter(strengthResult.level),
                    color = strengthUi.color,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.size(18.dp))

                AppDialogSecondaryButton(
                    text = regeneratePasswordText,
                    onClick = {
                        generatedPassword = generateStrongPassword(passwordLength)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}

private fun strengthUi(
    level: PasswordStrengthLevel,
    error: Color,
    ok: Color
): GeneratorPasswordStrengthUi {
    return when (level) {
        PasswordStrengthLevel.VERY_WEAK ->
            GeneratorPasswordStrengthUi(color = error)

        PasswordStrengthLevel.WEAK ->
            GeneratorPasswordStrengthUi(color = error)

        PasswordStrengthLevel.FAIR ->
            GeneratorPasswordStrengthUi(color = Color(0xFFCC8A00))

        PasswordStrengthLevel.GOOD ->
            GeneratorPasswordStrengthUi(color = ok)

        PasswordStrengthLevel.STRONG ->
            GeneratorPasswordStrengthUi(color = ok)
    }
}
