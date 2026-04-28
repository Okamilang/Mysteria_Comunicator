package com.okamilang.mysteria.urgent

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.okamilang.mysteria.ui.theme.LaitonClair
import com.okamilang.mysteria.ui.theme.Papier
import com.okamilang.mysteria.ui.theme.PapierClair
import com.okamilang.mysteria.ui.theme.RougeSceau
import com.okamilang.mysteria.ui.theme.RougeVif
import com.okamilang.mysteria.ui.theme.VertOcculte

@Composable
fun UrgentTransmissionScreen(
    fromCode: String,
    body: String,
    onRepondre: () -> Unit,
    onDifferer: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "halo")
    val scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-scale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A0A0A), Color(0xFF1A0404), Color(0xFF0A0202))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Halos pulsants empilés
        Box(
            modifier = Modifier
                .size(420.dp)
                .scale(scale)
                .background(RougeVif.copy(alpha = haloAlpha * 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(scale)
                .background(RougeVif.copy(alpha = haloAlpha * 0.25f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale)
                .background(RougeSceau.copy(alpha = haloAlpha * 0.5f), CircleShape)
        )

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .border(1.dp, Papier.copy(alpha = .5f))
                    .background(RougeSceau.copy(alpha = .6f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    "⚠ TRANSMISSION URGENTE ⚠",
                    color = Papier,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }

            // Grand sceau central
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFC44040), Color(0xFF8E2A2A), Color(0xFF3A0A0A))
                        ),
                        CircleShape
                    )
                    .border(2.dp, Papier.copy(alpha = .6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚜",
                    color = Papier,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp)
                )
            }

            Text(
                fromCode,
                color = LaitonClair,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Text(
                "tente de vous joindre par fréquence prioritaire",
                color = Papier,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (body.isNotBlank()) {
                Text(
                    "« $body »",
                    color = PapierClair,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Text(
                "〰 VIBRATION INTENSIVE EN COURS 〰",
                color = RougeVif,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )

            // Boutons d'action
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                BoutonAction(
                    icone = Icons.Default.CallEnd,
                    libelle = "Différer",
                    couleur = Color(0xFF3A3A3A),
                    onClick = onDifferer
                )
                BoutonAction(
                    icone = Icons.Default.Call,
                    libelle = "Répondre",
                    couleur = VertOcculte,
                    onClick = onRepondre
                )
            }
        }
    }
}

@Composable
private fun BoutonAction(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    libelle: String,
    couleur: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp).background(couleur, CircleShape).border(2.dp, Color.Black, CircleShape),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Papier)
        ) {
            Icon(icone, contentDescription = libelle, modifier = Modifier.size(32.dp))
        }
        Text(
            libelle.uppercase(),
            color = Papier.copy(alpha = .7f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
