package com.okamilang.mysteria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.okamilang.mysteria.data.Agent
import com.okamilang.mysteria.data.AuthRepository
import com.okamilang.mysteria.ui.components.PapierSurface
import com.okamilang.mysteria.ui.theme.Encre
import com.okamilang.mysteria.ui.theme.EncreClair
import com.okamilang.mysteria.ui.theme.LaitonSombre
import com.okamilang.mysteria.ui.theme.PapierClair
import com.okamilang.mysteria.ui.theme.PapierFonce
import com.okamilang.mysteria.ui.theme.RougeSceau

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistreScreen(
    onClose: () -> Unit,
    onSelectionner: (uid: String, codeName: String) -> Unit
) {
    var agents by remember { mutableStateOf<List<Agent>>(emptyList()) }
    var chargement by remember { mutableStateOf(true) }
    var erreur by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        AuthRepository.listOtherAgents().fold(
            onSuccess = { agents = it },
            onFailure = { erreur = it.message ?: "Registre inaccessible" }
        )
        chargement = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Registre", style = MaterialTheme.typography.headlineMedium, color = Encre)
                        Text(
                            "★ Annuaire des Agents ★",
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
        containerColor = PapierClair
    ) { padding ->
        PapierSurface(
            modifier = Modifier.fillMaxSize().padding(padding),
            padding = PaddingValues(12.dp)
        ) {
            when {
                chargement -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RougeSceau)
                }
                erreur != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        erreur!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RougeSceau,
                        textAlign = TextAlign.Center
                    )
                }
                agents.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucun autre agent inscrit pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreClair,
                        textAlign = TextAlign.Center
                    )
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(agents, key = { it.uid }) { agent ->
                        FicheAgent(
                            agent = agent,
                            onClick = { onSelectionner(agent.uid, agent.codeName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FicheAgent(agent: Agent, onClick: () -> Unit) {
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Portrait(agent.codeName)
            Column(Modifier.weight(1f)) {
                Text(
                    agent.codeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Encre
                )
                if (agent.email.isNotBlank()) {
                    Text(
                        agent.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = EncreClair
                    )
                }
            }
        }
    }
}

@Composable
private fun Portrait(code: String) {
    val initiales = code.trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(54.dp, 64.dp)
            .background(LaitonSombre)
            .border(1.dp, Encre),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initiales,
            style = MaterialTheme.typography.titleLarge,
            color = PapierClair
        )
    }
}
