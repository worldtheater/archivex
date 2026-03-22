package com.worldtheater.archive.feature.note_list.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldtheater.archive.security.PASSWORD_STRENGTH_MAX_SCORE
import com.worldtheater.archive.security.PasswordStrengthLevel
import com.worldtheater.archive.security.evaluateGeneratedPasswordStrength
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.appOnSurfaceA05
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.theme.appOnSurfaceA30
import com.worldtheater.archive.ui.theme.appOnSurfaceA50
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.IosAlert

@Composable
fun PasswordDialog(
    title: String,
    description: String? = null,
    enableStrengthCheck: Boolean = true,
    confirmText: String,
    cancelText: String,
    passwordLabel: String,
    confirmPasswordLabel: String,
    passwordMismatchText: String,
    passwordPolicyNotMetText: String,
    passwordIllegalCharsFormatter: @Composable (String) -> String,
    passwordStrengthFormatter: @Composable (PasswordStrengthLevel) -> String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    // Reverting to AppDialog to solve focus issues when returning from background (e.g. File Picker)
    AppDialog(onDismissRequest = onDismiss, enableSlideAnimation = false) { triggerClose ->
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val confirmFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        val passwordValid = password.length >= MIN_PASSWORD_LENGTH
        val confirmPasswordValid = confirmPassword.length >= MIN_PASSWORD_LENGTH
        val passwordCharsLegal = isPasswordCharsLegal(password)
        val passwordsMatch = password == confirmPassword
        val strength = remember(password) { evaluateGeneratedPasswordStrength(password) }
        val strengthPass = strength.score >= REQUIRED_STRENGTH_SCORE
        val invalidPolicyText = passwordPolicyNotMetText
        val invalidCharsText = passwordIllegalCharsFormatter(summarizeUnsupportedChars(password))

        val confirmErrorText = when {
            passwordCharsLegal.not() -> invalidCharsText
            confirmPassword.isBlank() -> null
            confirmPasswordValid.not() -> invalidPolicyText
            passwordsMatch.not() -> passwordMismatchText
            strengthPass.not() -> invalidPolicyText
            else -> null
        }

        val canConfirm =
            passwordValid && passwordCharsLegal && passwordsMatch && strengthPass

        val visualTransformation: VisualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }

        var isPasswordFocused by remember { mutableStateOf(false) }
        var isConfirmFocused by remember { mutableStateOf(false) }

        val customTextStyle =
            MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        val transparentColor = Color.Transparent
        val focusedColor = appOnSurfaceA05()
        val placeholderColor = appOnSurfaceA30()
        val borderColor = appOnSurfaceA10()

        IosAlert(
            title = title,
            description = description,
            primaryText = confirmText,
            primaryButtonEnabled = canConfirm,
            onPrimaryClick = {
                if (canConfirm) {
                    triggerClose { onConfirm(password) }
                }
            },
            secondaryText = cancelText,
            onSecondaryClick = { triggerClose { onDismiss() } }
        ) {
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        passwordLabel,
                        style = customTextStyle.copy(color = placeholderColor)
                    )
                },
                textStyle = customTextStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isPasswordFocused = it.isFocused },
                singleLine = true,
                visualTransformation = visualTransformation,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = appOnSurfaceA50()
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isPasswordFocused) focusedColor else transparentColor,
                    unfocusedContainerColor = if (isPasswordFocused) focusedColor else transparentColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorContainerColor = if (isPasswordFocused) focusedColor else transparentColor,
                    errorIndicatorColor = transparentColor,
                    disabledIndicatorColor = transparentColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        confirmFocusRequester.requestFocus()
                    }
                ),
                shape = SmoothRoundedCornerShape(18.dp)
            )

            Spacer(modifier = Modifier.size(12.dp))

            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = {
                    Text(
                        confirmPasswordLabel,
                        style = customTextStyle.copy(color = placeholderColor)
                    )
                },
                textStyle = customTextStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(confirmFocusRequester)
                    .onFocusChanged { isConfirmFocused = it.isFocused },
                singleLine = true,
                isError = confirmErrorText != null,
                visualTransformation = visualTransformation,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = appOnSurfaceA50()
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isConfirmFocused) focusedColor else transparentColor,
                    unfocusedContainerColor = if (isConfirmFocused) focusedColor else transparentColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorContainerColor = if (isConfirmFocused) focusedColor else transparentColor,
                    errorIndicatorColor = transparentColor,
                    disabledIndicatorColor = transparentColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canConfirm) {
                            triggerClose { onConfirm(password) }
                        }
                    }
                ),
                shape = SmoothRoundedCornerShape(18.dp)
            )

            if (confirmErrorText != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = confirmErrorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (enableStrengthCheck && password.isNotBlank()) {
                Spacer(modifier = Modifier.size(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = passwordStrengthFormatter(strength.level),
                        color = strengthColor(
                            score = strength.score,
                            error = MaterialTheme.colorScheme.error,
                            ok = MaterialTheme.colorScheme.primary
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    SegmentedStrengthBar(
                        score = strength.score,
                        modifier = Modifier.fillMaxWidth(),
                        activeColor = strengthColor(
                            score = strength.score,
                            error = MaterialTheme.colorScheme.error,
                            ok = MaterialTheme.colorScheme.primary
                        ),
                        inactiveColor = appOnSurfaceA10()
                    )
                }
            }

            Spacer(modifier = Modifier.size(18.dp))
        }
    }
}

private fun strengthColor(score: Int, error: Color, ok: Color): Color {
    return when {
        score <= 1 -> error
        score <= 3 -> Color(0xFFCC8A00)
        else -> ok
    }
}

private const val REQUIRED_STRENGTH_SCORE = 4
private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_UNSUPPORTED_CHARS_SHOWN = 6

private fun isPasswordCharsLegal(input: String): Boolean {
    return input.all(::isAllowedPasswordChar)
}

private fun isAllowedPasswordChar(ch: Char): Boolean {
    return ch in 'a'..'z' ||
            ch in 'A'..'Z' ||
            ch in '0'..'9' ||
            ch.code in 33..47 ||
            ch.code in 58..64 ||
            ch.code in 91..96 ||
            ch.code in 123..126
}

private fun summarizeUnsupportedChars(input: String): String {
    val unsupported = input.toList().filterNot(::isAllowedPasswordChar).distinct()
    if (unsupported.isEmpty()) return "-"
    val visible = unsupported.take(MAX_UNSUPPORTED_CHARS_SHOWN).map(::renderUnsupportedChar)
    return if (unsupported.size > MAX_UNSUPPORTED_CHARS_SHOWN) {
        visible.joinToString(", ") + ", ..."
    } else {
        visible.joinToString(", ")
    }
}

private fun renderUnsupportedChar(ch: Char): String {
    val code = "U+" + ch.code.toString(16).uppercase().padStart(4, '0')
    return when (ch) {
        ' ' -> "space($code)"
        '\t' -> "\\t($code)"
        '\n' -> "\\n($code)"
        '\r' -> "\\r($code)"
        else -> if (ch.isISOControl()) code else "$ch($code)"
    }
}

@Composable
private fun SegmentedStrengthBar(
    score: Int,
    modifier: Modifier = Modifier,
    activeColor: Color,
    inactiveColor: Color
) {
    val segments = PASSWORD_STRENGTH_MAX_SCORE
    Row(modifier = modifier) {
        repeat(segments) { index ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .size(height = 6.dp, width = 0.dp)
                    .background(
                        color = if (index < score) activeColor else inactiveColor,
                        shape = SmoothRoundedCornerShape(99.dp)
                    )
            )
            if (index < segments - 1) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
