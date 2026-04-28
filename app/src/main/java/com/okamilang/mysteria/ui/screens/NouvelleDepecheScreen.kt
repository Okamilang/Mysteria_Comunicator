package com.okamilang.mysteria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.okamilang.mysteria.data.MessagesRepository
import com.okamilang.mysteria.ui.components.BoutonLaiton
import com.okamilang.mysteria.ui.components.OrnementTrait
import com.okamilang.mysteria.ui.components.PapierSurface
import com.okamilang.mysteria.ui.theme.Encre
import com.okamilang.mysteria.ui.theme.EncreClair
import com.okamilang.mysteria.ui.theme.LaitonSombre
import com.okamilang.mysteria.ui.theme.PapierClair
import com.okamilang.mysteria.ui.theme.PapierFonce
import com.okamilang.mysteria.ui.theme.RougeSceau
import com.okamilang.mysteria.ui.theme.VertOcculte
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NouvelleDepecheScreen(
    onClose: () -> Unit,
    destinataireInitial: String? = null
) {
    var destinataire by remember { mutableStateOf(destinataireInitial.orEmpty()) }
    var corps by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackOk by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle dépêche", style = MaterialTheme.typography.headlineMedium, color = Encre) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Encre)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PapierFonce,
                    titleContentColor = Encre,
                    navigationIconContentColor = Encre
                )
            )
        },
        containerColor = PapierClair
    ) { padding ->
        PapierSurface(
            modifier = Modifier.fillMaxSize().padding(padding),
            padding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OrnementTrait("À l'attention de")

                OutlinedTextField(
                    value = destinataire,
                    onValueChange = { destinataire = it },
                    label = { Text("Nom de code du destinataire") },
                    modifier = Modifier.fillMaxWidth().background(PapierClair),
                    singleLine = true,
                    colors = champsCouleurs()
                )

                OrnementTrait("Corps de la dépêche")

                OutlinedTextField(
                    value = corps,
                    onValueChange = { corps = it },
                    placeholder = { Text("Rédigez votre dépêche...", color = EncreClair) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(PapierClair),
                    minLines = 6,
                    colors = champsCouleurs()
                )

                feedback?.let { msg ->
                    Text(
                        msg,
                        color = if (feedbackOk) VertOcculte else RougeSceau,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                BoutonLaiton(
                    text = "Sceller et expédier",
                    onClick = {
                        feedback = null
                        loading = true
                        scope.launch {
                            val res = MessagesRepository.sendMessage(destinataire, corps)
                            loading = false
                            res.fold(
                                onSuccess = {
                                    feedbackOk = true
                                    feedback = "Dépêche expédiée."
                                    corps = ""
                                },
                                onFailure = {
                                    feedbackOk = false
                                    feedback = it.message ?: "L'expédition a échoué"
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !loading && destinataire.isNotBlank() && corps.isNotBlank()
                )

                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = RougeSceau,
                        trackColor = LaitonSombre
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun champsCouleurs() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Encre,
    unfocusedTextColor = Encre,
    focusedBorderColor = Encre,
    unfocusedBorderColor = LaitonSombre,
    cursorColor = Encre,
    focusedContainerColor = PapierClair,
    unfocusedContainerColor = PapierClair,
    focusedLabelColor = Encre,
    unfocusedLabelColor = EncreClair
)
