package com.okamilang.mysteria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.okamilang.mysteria.data.AuthRepository
import com.okamilang.mysteria.ui.components.BoutonLaiton
import com.okamilang.mysteria.ui.components.OrnementTrait
import com.okamilang.mysteria.ui.components.PapierSurface
import com.okamilang.mysteria.ui.theme.Encre
import com.okamilang.mysteria.ui.theme.EncreClair
import com.okamilang.mysteria.ui.theme.LaitonSombre
import com.okamilang.mysteria.ui.theme.PapierClair
import com.okamilang.mysteria.ui.theme.RougeSceau
import kotlinx.coroutines.launch

@Composable
fun IdentificationScreen(onAuthenticated: () -> Unit) {
    var modeInscription by remember { mutableStateOf(false) }
    var codeName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    PapierSurface(
        modifier = Modifier.fillMaxSize(),
        padding = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "MYSTERIA\nCOMMUNICATOR",
                style = MaterialTheme.typography.headlineLarge,
                color = Encre,
                textAlign = TextAlign.Center
            )
            Text(
                text = "★ Agence des Affaires Extraordinaires ★",
                style = MaterialTheme.typography.labelMedium,
                color = RougeSceau,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
            OrnementTrait(if (modeInscription) "Engagement" else "Identification")

            if (modeInscription) {
                ChampPapier(
                    libelle = "Nom de code",
                    valeur = codeName,
                    onChange = { codeName = it }
                )
            }

            ChampPapier(
                libelle = "Adresse de correspondance",
                valeur = email,
                onChange = { email = it },
                keyboardType = KeyboardType.Email
            )

            ChampPapier(
                libelle = "Sceau d'agent",
                valeur = password,
                onChange = { password = it },
                keyboardType = KeyboardType.Password,
                masque = true
            )

            erreur?.let {
                Text(
                    text = it,
                    color = RougeSceau,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            BoutonLaiton(
                text = if (modeInscription) "Engager l'agent" else "Prendre ses fonctions",
                onClick = {
                    erreur = null
                    loading = true
                    scope.launch {
                        val res = if (modeInscription)
                            AuthRepository.signUp(email, password, codeName)
                        else
                            AuthRepository.signIn(email, password)

                        loading = false
                        res.fold(
                            onSuccess = { onAuthenticated() },
                            onFailure = { erreur = it.message ?: "Identification refusée" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !loading && email.isNotBlank() && password.isNotBlank() &&
                          (!modeInscription || codeName.isNotBlank())
            )

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = RougeSceau,
                    trackColor = LaitonSombre
                )
            }

            Spacer(Modifier.height(4.dp))

            TextButton(onClick = { modeInscription = !modeInscription; erreur = null }) {
                Text(
                    text = if (modeInscription) "Déjà engagé ? Identifiez-vous"
                           else "Nouvel agent ? Demander son matricule",
                    color = EncreClair,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "— Sub Rosa, Veritas —",
                color = EncreClair,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChampPapier(
    libelle: String,
    valeur: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    masque: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = libelle.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = EncreClair
        )
        OutlinedTextField(
            value = valeur,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(PapierClair),
            singleLine = true,
            visualTransformation = if (masque) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Encre,
                unfocusedTextColor = Encre,
                focusedBorderColor = Encre,
                unfocusedBorderColor = LaitonSombre,
                cursorColor = Encre,
                focusedContainerColor = PapierClair,
                unfocusedContainerColor = PapierClair
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Encre)
        )
    }
}
