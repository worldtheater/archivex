package com.worldtheater.archive.feature.note_list.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.theme.appOnSurfaceA30
import com.worldtheater.archive.ui.widget.AppDialog
import com.worldtheater.archive.ui.widget.IosAlert


@Composable
fun NewNoteDialog(
    title: String,
    primaryText: String,
    secondaryText: String,
    placeholder: String = "",
    defaultPlaceholderText: String,
    onDismiss: () -> Unit,
    onNegativeClick: () -> Unit,
    onPositiveClick: (String) -> Unit
) {
    AppDialog(onDismissRequest = onDismiss, enableSlideAnimation = false) { triggerClose ->
        val onNegativeClickState = rememberUpdatedState(onNegativeClick)
        var inputText by rememberSaveable { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        IosAlert(
            title = title,
            primaryText = primaryText,
            titleOnlyBottomSpace = 0.dp,
            onPrimaryClick = { triggerClose { onPositiveClick(inputText) } },
            primaryButtonEnabled = inputText.isNotBlank(),
            secondaryText = secondaryText,
            onSecondaryClick = { triggerClose { onNegativeClickState.value.invoke() } }
        ) {
//            Spacer(modifier = Modifier.height(8.dp))

            val textStyle =
                MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            val placeholderColor = appOnSurfaceA30()

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (placeholder.isNotBlank()) placeholder else defaultPlaceholderText,
                        color = placeholderColor,
                        style = textStyle
                    )
                },
                textStyle = textStyle,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                keyboardActions = KeyboardActions(),
                singleLine = false,
//                maxLines = 10,
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    }
}
