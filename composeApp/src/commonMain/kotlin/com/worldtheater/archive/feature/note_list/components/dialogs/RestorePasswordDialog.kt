package com.worldtheater.archive.feature.note_list.components.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import com.worldtheater.archive.ui.shape.SmoothRoundedCornerShape
import com.worldtheater.archive.ui.theme.appOnSurfaceA05
import com.worldtheater.archive.ui.theme.appOnSurfaceA10
import com.worldtheater.archive.ui.theme.appOnSurfaceA30
import com.worldtheater.archive.ui.theme.appOnSurfaceA50
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.IosAlert
import kotlinx.coroutines.delay

@Composable
fun RestorePasswordDialog(
    title: String,
    description: String? = null,
    confirmText: String,
    cancelText: String,
    passwordLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    AppDialog(onDismissRequest = onDismiss, enableSlideAnimation = false) { triggerClose ->
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        var isFocused by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(200)
            focusRequester.requestFocus()
        }

        IosAlert(
            title = title,
            description = description,
            primaryText = confirmText,
            primaryButtonEnabled = password.isNotEmpty(),
            onPrimaryClick = {
                if (password.isNotEmpty()) {
                    triggerClose { onConfirm(password) }
                }
            },
            secondaryText = cancelText,
            onSecondaryClick = { triggerClose { onDismiss() } }
        ) {
            val visualTransformation: VisualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            }

            val customTextStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            val transparentColor = Color.Transparent
            val focusedColor = appOnSurfaceA05()
            val placeholderColor = appOnSurfaceA30()
            val borderColor = appOnSurfaceA10()

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
                    .onFocusChanged { isFocused = it.isFocused },
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
                    focusedContainerColor = if (isFocused) focusedColor else transparentColor,
                    unfocusedContainerColor = if (isFocused) focusedColor else transparentColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
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
                        if (password.isNotEmpty()) {
                            triggerClose { onConfirm(password) }
                        }
                    }
                ),
                shape = SmoothRoundedCornerShape(18.dp)
            )

            Spacer(modifier = Modifier.size(18.dp))
        }
    }
}
