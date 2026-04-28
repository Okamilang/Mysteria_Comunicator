package com.okamilang.mysteria.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.okamilang.mysteria.audio.AudioPlayer
import com.okamilang.mysteria.audio.AudioRecorder
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
    val context = LocalContext.current
    val messages by remember(otherUid) { MessagesRepository.observeConversation(otherUid) }
        .collectAsState(initial = emptyList<Message>())
    val meUid = AuthRepository.currentUser?.uid.orEmpty()
    var saisie by remember { mutableStateOf("") }
    var modeUrgent by remember { mutableStateOf(false) }
    var envoiEnCours by remember { mutableStateOf(false) }
    var enregistrement by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val recorder = remember { AudioRecorder(context) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val urgent = modeUrgent
        envoiEnCours = true
        scope.launch {
            val res = MessagesRepository.sendImage(context, otherCode, uri, urgent = urgent)
            envoiEnCours = false
            res.fold(
                onSuccess = { modeUrgent = false },
                onFailure = { erreur = it.message ?: "Échec d'expédition de la plaque" }
            )
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { accordee ->
        if (!accordee) erreur = "Accès au micro refusé"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(otherCode, style = MaterialTheme.typography.headlineMedium, color = Encre)
                        Text("★ Conversation ★", style = MaterialTheme.typography.labelSmall, color = EncreClair)
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
                enregistrement = enregistrement,
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
                },
                onJoindreImage = {
                    imageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onMaintenirMic = {
                    val accordee = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!accordee) {
                        micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return@BarreSaisie
                    }
                    if (recorder.start() != null) {
                        enregistrement = true
                    } else {
                        erreur = "Impossible de démarrer l'enregistrement"
                    }
                },
                onRelacherMic = {
                    if (!enregistrement) return@BarreSaisie
                    enregistrement = false
                    val resultat = recorder.stop()
                    if (resultat == null) {
                        erreur = "Cylindre trop court (minimum 0,5 s)"
                        return@BarreSaisie
                    }
                    val (fichier, duree) = resultat
                    val urgent = modeUrgent
                    envoiEnCours = true
                    scope.launch {
                        val res = MessagesRepository.sendAudio(otherCode, fichier, duree, urgent)
                        envoiEnCours = false
                        fichier.delete()
                        res.fold(
                            onSuccess = { modeUrgent = false },
                            onFailure = { erreur = it.message ?: "Échec d'expédition du cylindre" }
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
                when (msg.attachmentType) {
                    "image" -> ContenuImage(msg)
                    "audio" -> ContenuAudio(msg)
                    else -> Text(msg.body, style = MaterialTheme.typography.bodyMedium, color = Encre)
                }
                Text(
                    formatHeure(msg.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = EncreClair,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ContenuImage(msg: Message) {
    Column {
        AsyncImage(
            model = msg.attachmentUrl,
            contentDescription = "Plaque photographique",
            modifier = Modifier
                .widthIn(max = 256.dp)
                .height(180.dp)
                .background(LaitonSombre),
            contentScale = ContentScale.Crop
        )
        Text(
            "★ Plaque photographique ★",
            style = MaterialTheme.typography.labelSmall,
            color = EncreClair,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (msg.body.isNotBlank()) {
            Text(
                msg.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Encre,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ContenuAudio(msg: Message) {
    val url = msg.attachmentUrl ?: return
    var enLecture by remember(url) { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(min = 200.dp)
    ) {
        FilledIconButton(
            onClick = {
                if (enLecture) {
                    AudioPlayer.stop()
                    enLecture = false
                } else {
                    AudioPlayer.play(url) { enLecture = false }
                    enLecture = true
                }
            },
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = RougeSceau,
                contentColor = PapierClair
            )
        ) {
            Icon(
                if (enLecture) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (enLecture) "Arrêter" else "Lire le cylindre"
            )
        }
        WaveformDeco(modifier = Modifier.weight(1f).height(20.dp))
        Text(
            formatDuree(msg.audioDurationMs ?: 0L),
            style = MaterialTheme.typography.labelSmall,
            color = EncreClair
        )
    }
    Text(
        "★ Cylindre phonographique ★",
        style = MaterialTheme.typography.labelSmall,
        color = EncreClair,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun WaveformDeco(modifier: Modifier = Modifier) {
    val hauteurs = remember { listOf(4, 9, 14, 7, 11, 18, 12, 8, 15, 10, 6, 13, 17, 9, 5, 12, 8) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        hauteurs.forEach { h ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h.dp)
                    .background(EncreClair)
            )
        }
    }
}

private fun formatDuree(ms: Long): String {
    val s = (ms / 1000).toInt()
    val mm = s / 60
    val ss = s % 60
    return "%d:%02d".format(mm, ss)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarreSaisie(
    texte: String,
    onTexte: (String) -> Unit,
    modeUrgent: Boolean,
    onBasculerUrgent: () -> Unit,
    envoiEnCours: Boolean,
    enregistrement: Boolean,
    onEnvoyer: () -> Unit,
    onJoindreImage: () -> Unit,
    onMaintenirMic: () -> Unit,
    onRelacherMic: () -> Unit
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
            if (enregistrement) {
                Text(
                    text = "🎙 Gravure du cylindre en cours — relâcher pour expédier",
                    color = RougeSceau,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledIconButton(
                    onClick = onBasculerUrgent,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (modeUrgent) RougeSceau else PapierClair,
                        contentColor = if (modeUrgent) PapierClair else LaitonSombre
                    )
                ) { Icon(Icons.Default.PriorityHigh, contentDescription = "Mode urgent") }

                FilledIconButton(
                    onClick = onJoindreImage,
                    enabled = !envoiEnCours && !enregistrement,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PapierClair,
                        contentColor = LaitonSombre
                    )
                ) { Icon(Icons.Default.Image, contentDescription = "Joindre une plaque") }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (enregistrement) RougeSceau else PapierClair,
                            CircleShape
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onMaintenirMic()
                                    awaitRelease()
                                    onRelacherMic()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Graver un cylindre (maintenir)",
                        tint = if (enregistrement) PapierClair else LaitonSombre
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
                    enabled = !envoiEnCours && !enregistrement && texte.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = RougeSceau,
                        contentColor = PapierClair,
                        disabledContainerColor = LaitonSombre,
                        disabledContentColor = PapierClair
                    )
                ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Expédier") }
            }
        }
    }
}
