package com.okamilang.mysteria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.okamilang.mysteria.data.AuthRepository
import com.okamilang.mysteria.data.Message
import com.okamilang.mysteria.data.MessagesRepository
import com.okamilang.mysteria.ui.components.PapierSurface
import com.okamilang.mysteria.ui.theme.Encre
import com.okamilang.mysteria.ui.theme.EncreClair
import com.okamilang.mysteria.ui.theme.LaitonSombre
import com.okamilang.mysteria.ui.theme.Papier
import com.okamilang.mysteria.ui.theme.PapierClair
import com.okamilang.mysteria.ui.theme.PapierFonce
import com.okamilang.mysteria.ui.theme.RougeSceau
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    otherUid: String,
    otherCode: String,
    onClose: () -> Unit
) {
    val messages by remember(otherUid) { MessagesRepository.observeConversation(otherUid) }
        .collectAsState(initial = emptyList<Message>())
    val meUid = AuthRepository.currentUser?.uid.orEmpty()
    var saisie by remember { mutableStateOf("") }
    var modeUrgent by remember { mutableStateOf(false) }
    var envoiEnCours by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Faire défiler vers le bas quand un nouveau message arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            otherCode,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Encre
                        )
                        Text(
                            "★ Conversation ★",
                            style = MaterialTheme.typography.labelSmall,
                            color = EncreClair
                        )
                    }
                },
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
        bottomBar = {
            BarreSaisie(
                texte = saisie,
                onTexte = { saisie = it },
                modeUrgent = modeUrgent,
                onBasculerUrgent = { modeUrgent = !modeUrgent },
                envoiEnCours = envoiEnCours,
                onEnvoyer = {
                    if (saisie.isBlank()) return@BarreSaisie
                    val corps = saisie.trim()
                    val urgent = modeUrgent
                    erreur = null
                    envoiEnCours = true
                    scope.launch {
                        val res = MessagesRepository.sendMessage(otherCode, corps, urgent)
                        envoiEnCours = false
                        res.fold(
                            onSuccess = {
                                saisie = ""
                                modeUrgent = false
                            },
                            onFailure = { erreur = it.message ?: "Échec de l'expédition" }
                        )
                    }
                }
            )
        },
        containerColor = PapierClair
    ) { padding ->
        PapierSurface(
            modifier = Modifier.fillMaxSize().padding(padding),
            padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                if (messages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Aucune dépêche échangée.\nÀ vous d'ouvrir le bal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EncreClair,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            BulleMessage(msg = msg, deMoi = msg.fromUid == meUid)
                        }
                    }
                }

                erreur?.let {
                    Text(
                        it,
                        color = RougeSceau,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BulleMessage(msg: Message, deMoi: Boolean) {
    val couleurFond = if (deMoi) Papier else PapierClair
    val alignement = if (deMoi) Alignment.End else Alignment.Start
    val forme = if (deMoi)
        RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignement
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(couleurFond, forme)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    msg.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Encre
                )
                Text(
                    formatHeure(msg.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = EncreClair,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarreSaisie(
    texte: String,
    onTexte: (String) -> Unit,
    modeUrgent: Boolean,
    onBasculerUrgent: () -> Unit,
    envoiEnCours: Boolean,
    onEnvoyer: () -> Unit
) {
    Surface(color = PapierFonce, tonalElevation = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (modeUrgent) {
                Text(
                    text = "⚠ MODE URGENT — vibration intensive chez le destinataire ⚠",
                    color = RougeSceau,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledIconButton(
                    onClick = onBasculerUrgent,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (modeUrgent) RougeSceau else PapierClair,
                        contentColor = if (modeUrgent) PapierClair else LaitonSombre
                    )
                ) {
                    Icon(
                        Icons.Default.PriorityHigh,
                        contentDescription = if (modeUrgent) "Désactiver mode urgent" else "Activer mode urgent"
                    )
                }
                OutlinedTextField(
                    value = texte,
                    onValueChange = onTexte,
                    placeholder = {
                        Text(
                            if (modeUrgent) "Dépêche urgente..." else "Rédigez votre dépêche...",
                            color = EncreClair
                        )
                    },
                    modifier = Modifier.weight(1f).background(PapierClair),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Encre,
                        unfocusedTextColor = Encre,
                        focusedBorderColor = if (modeUrgent) RougeSceau else Encre,
                        unfocusedBorderColor = if (modeUrgent) RougeSceau else LaitonSombre,
                        cursorColor = Encre,
                        focusedContainerColor = PapierClair,
                        unfocusedContainerColor = PapierClair
                    ),
                    maxLines = 4
                )
                FilledIconButton(
                    onClick = onEnvoyer,
                    enabled = !envoiEnCours && texte.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = RougeSceau,
                        contentColor = PapierClair,
                        disabledContainerColor = LaitonSombre,
                        disabledContentColor = PapierClair
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Expédier")
                }
            }
        }
    }
}
