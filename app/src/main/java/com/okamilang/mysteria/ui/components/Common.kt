package com.okamilang.mysteria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.okamilang.mysteria.ui.theme.Encre
import com.okamilang.mysteria.ui.theme.LaitonClair
import com.okamilang.mysteria.ui.theme.LaitonSombre
import com.okamilang.mysteria.ui.theme.Papier
import com.okamilang.mysteria.ui.theme.PapierClair
import com.okamilang.mysteria.ui.theme.RougeSceau

/** Surface "papier sépia" avec bordure encre, base de tous les écrans. */
@Composable
fun PapierSurface(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(PapierClair, Papier)))
            .padding(padding)
    ) {
        content()
    }
}

/** Bouton style "plaque de laiton" avec dégradé et bordure foncée. */
@Composable
fun BoutonLaiton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = LaitonClair,
            contentColor = Encre,
            disabledContainerColor = LaitonSombre,
            disabledContentColor = Papier
        )
    ) {
        Text(
            text = text.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
        )
    }
}

/** Trait orné avec libellé central, type "⚜ titre ⚜". */
@Composable
fun OrnementTrait(
    libelle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = LaitonSombre,
            thickness = 1.dp
        )
        Text(
            text = "⚜ $libelle ⚜",
            color = Encre,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = LaitonSombre,
            thickness = 1.dp
        )
    }
}

/** Petit sceau de cire rond avec une initiale. */
@Composable
fun Sceau(initiale: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(34.dp)
            .background(RougeSceau, shape = androidx.compose.foundation.shape.CircleShape)
            .border(1.dp, Encre, androidx.compose.foundation.shape.CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initiale.take(1).uppercase(),
            color = Papier,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
    }
}
