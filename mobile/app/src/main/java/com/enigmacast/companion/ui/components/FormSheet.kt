package com.enigmacast.companion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.enigmacast.companion.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.enigmacast.companion.protocol.FieldType
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.theme.Ink

/**
 * Verschijnt zodra de TV een tekstveld opent (Settings, zoeken, wachtwoordvelden).
 * De tekst gaat live naar de TV; Klaar sluit het veld op de TV, Annuleren doet hetzelfde
 * zonder de tekst te bewaren.
 */
@Composable
fun FormSheet(vm: AppViewModel) {
    val form = vm.form ?: return
    val fields = form.fields
    if (fields.isEmpty()) return
    val focusRequesters = remember(form.formId) { List(fields.size) { FocusRequester() } }

    Dialog(
        onDismissRequest = vm::cancelForm,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Ink.CardSolid,
            modifier = Modifier.fillMaxWidth().padding(20.dp).imePadding(),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "TYP OP JE TELEFOON",
                    color = Ink.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                )
                Text(form.title, color = Ink.Text, style = MaterialTheme.typography.titleLarge)
                Text(
                    "De tekst verschijnt meteen op de tv.",
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
                fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = vm.formValues[field.id].orEmpty(),
                        onValueChange = { vm.updateFormField(field.id, it) },
                        label = { Text(field.label) },
                        singleLine = field.type != FieldType.MULTILINE,
                        visualTransformation = if (field.masked) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when (field.type) {
                                FieldType.HOST -> KeyboardType.Uri
                                FieldType.NUMBER -> KeyboardType.Number
                                FieldType.PASSWORD -> KeyboardType.Password
                                FieldType.TEXT, FieldType.MULTILINE,
                                FieldType.SELECT, FieldType.TOGGLE -> KeyboardType.Text
                            },
                            imeAction = if (index == fields.lastIndex) ImeAction.Done else ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onDone = { vm.submitForm() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Ink.Accent,
                            unfocusedBorderColor = Ink.Border,
                            focusedLabelColor = Ink.Accent,
                            unfocusedLabelColor = Ink.Subtle,
                            cursorColor = Ink.Accent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[index]),
                    )
                    if (field.masked) {
                        Text(
                            "Laat leeg om het huidige wachtwoord te behouden.",
                            color = Ink.Subtle,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    form.errors[field.id]?.let { error ->
                        Text(error, color = Ink.Danger, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = vm::cancelForm) {
                        Text("Annuleren", color = Ink.Muted)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = vm::submitForm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ink.Accent,
                            contentColor = Ink.OnAccent,
                        ),
                    ) {
                        Text("Klaar")
                    }
                }
            }
        }
    }

    LaunchedEffect(form.formId) {
        withFrameNanos { }
        focusRequesters.firstOrNull()?.requestFocus()
    }
}
