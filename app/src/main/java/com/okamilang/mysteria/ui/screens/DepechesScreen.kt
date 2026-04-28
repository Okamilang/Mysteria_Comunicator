package com.okamilang.mysteria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.okamilang.mysteria.data.AuthRepository
import com.okamilang.mysteria.data.ConversationSummary
import com.okamilang.mysteria.data.Message
import com.okamilang.mysteria.data.MessagesRepository
import com.okamilang.mysteria.data.groupConversations
import com.okamilang.mysteria.ui.components.PapierSurface
import com.okamilang.mysteria.ui.theme.Encre
import com.okamilang.mysteria.ui.theme.EncreClair
import com.okamilang.mysteria.ui.theme.LaitonSombre
import com.okamilang.mysteria.ui.theme.PapierClair
import com.okamilang.mysteria.ui.theme.PapierFonce
import com.okamilang.mysteria.ui.theme.RougeSceau
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepechesScreen(
    onSignOut: () -> Unit,
    onOuvrirConversation: (otherUid: String, otherCode: String) -> Unit,
    onOuvrirRegistre: () -> Unit
) {
    val messages by remember { MessagesRepository.observeAllMyMessages() }
        .collectAsState(initial = emptyList<Message>())
    val meUid = AuthRepository.currentUser?.uid.orEmpty()
    val conversations = remember(messages, meUid) { groupConversations(messages, meUid) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dépêches",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Encre
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PapierFonce,
                    titleContentColor = Encre,
                    actionIconContentColor = Encre
                ),
                actions = {
                    IconButton(onClick = onOuvrirRegistre) {
                        Icon(Icons.Default.Group, contentDescription = "Registre", tint = Encre)
                    }
                    IconButton(onClick = { AuthRepository.signOut(); onSignOut() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Quitter ses fonctions",
                            tint = Encre
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOuvrirRegistre,
                containerColor = RougeSceau,
                contentColor = PapierClair,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Rédiger une dépêche")
            }
        },
        containerColor = PapierClair
    ) { padding ->
        PapierSurface(
            modifier = Modifier.fillMaxSize().padding(padding),
            padding = PaddingValues(12.dp)
        ) {
            if (conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucune correspondance.\nUtilisez le Registre pour entrer en contact.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreClair,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(conversations, key = { it.otherUid }) { conv ->
                        ConversationItem(
                            conv = conv,
                            onClick = { onOuvrirConversation(conv.otherUid, conv.otherCode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(conv: ConversationSummary, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PapierClair)
            .border(1.dp, LaitonSombre, RectangleShape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SceauInitiale(conv.otherCode)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        conv.otherCode,
                        style = MaterialTheme.typography.titleMedium,
                        color = Encre,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatHeure(conv.dernierEnvoiMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = EncreClair
                    )
                }
                Text(
                    text = (if (conv.dernierEstDeMoi) "→ " else "") + conv.dernierMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreClair,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun SceauInitiale(code: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(RougeSceau, CircleShape)
            .border(1.dp, Encre, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            code.firstOrNull()?.uppercase() ?: "?",
            color = PapierClair,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

internal fun formatHeure(timestampMs: Long): String {
    if (timestampMs == 0L) return "—"
    val now = System.currentTimeMillis()
    val date = Date(timestampMs)
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date(now))
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(date)
    return if (today == day)
        SimpleDateFormat("HH'h'mm", Locale.FRANCE).format(date)
    else
        SimpleDateFormat("dd MMM", Locale.FRANCE).format(date)
}
