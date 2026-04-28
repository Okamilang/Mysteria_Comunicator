package com.okamilang.mysteria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
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
    onNouvelleDepeche: () -> Unit
) {
    val messages by remember { MessagesRepository.observeInbox() }
        .collectAsState(initial = emptyList<Message>())

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
                onClick = onNouvelleDepeche,
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
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucune dépêche reçue.\nLa correspondance se développe avec le temps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreClair,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { TelegrammeItem(it) }
                }
            }
        }
    }
}

@Composable
private fun TelegrammeItem(m: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PapierClair)
            .border(1.dp, LaitonSombre, RectangleShape)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SceauInitiale(m.fromCode)
                Text(
                    m.fromCode,
                    style = MaterialTheme.typography.titleMedium,
                    color = Encre,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatHeure(m.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = EncreClair
                )
            }
            HorizontalDivider(color = LaitonSombre.copy(alpha = .5f))
            Text(
                m.body,
                style = MaterialTheme.typography.bodyMedium,
                color = EncreClair
            )
        }
    }
}

@Composable
private fun SceauInitiale(code: String) {
    Box(
        modifier = Modifier
            .size(34.dp)
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

private fun formatHeure(timestampMs: Long): String {
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
