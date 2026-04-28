package com.okamilang.mysteria.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.okamilang.mysteria.data.AuthRepository
import com.okamilang.mysteria.ui.screens.ConversationScreen
import com.okamilang.mysteria.ui.screens.DepechesScreen
import com.okamilang.mysteria.ui.screens.IdentificationScreen
import com.okamilang.mysteria.ui.screens.RegistreScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val IDENTIFICATION = "identification"
    const val DEPECHES = "depeches"
    const val REGISTRE = "registre"
    const val CONVERSATION = "conversation/{uid}/{code}"

    fun conversation(uid: String, code: String): String {
        val u = URLEncoder.encode(uid, "UTF-8")
        val c = URLEncoder.encode(code, "UTF-8")
        return "conversation/$u/$c"
    }
}

/** Demande d'ouverture directe d'une conversation (deep-link interne). */
data class OuvertureConversation(val uid: String, val code: String)

@Composable
fun MysteriaApp(
    ouvertureConversation: OuvertureConversation? = null,
    onOuvertureConsommee: () -> Unit = {}
) {
    val navController = rememberNavController()
    val user by remember { AuthRepository.authStateFlow() }
        .collectAsState(initial = AuthRepository.currentUser)

    val start = if (user == null) Routes.IDENTIFICATION else Routes.DEPECHES

    // Quand un deep-link "ouvrir conversation" arrive (depuis l'écran
    // de Transmission Urgente qui tape Répondre), on navigue dès que
    // l'utilisateur est authentifié.
    LaunchedEffect(ouvertureConversation, user) {
        val ouv = ouvertureConversation ?: return@LaunchedEffect
        if (user == null) return@LaunchedEffect
        navController.navigate(Routes.conversation(ouv.uid, ouv.code)) {
            // On vide la pile pour atterrir directement dans la conversation.
            popUpTo(Routes.DEPECHES) { inclusive = false }
        }
        onOuvertureConsommee()
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.IDENTIFICATION) {
            IdentificationScreen(
                onAuthenticated = {
                    navController.navigate(Routes.DEPECHES) {
                        popUpTo(Routes.IDENTIFICATION) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DEPECHES) {
            DepechesScreen(
                onSignOut = {
                    navController.navigate(Routes.IDENTIFICATION) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOuvrirConversation = { uid, code ->
                    navController.navigate(Routes.conversation(uid, code))
                },
                onOuvrirRegistre = {
                    navController.navigate(Routes.REGISTRE)
                }
            )
        }
        composable(Routes.REGISTRE) {
            RegistreScreen(
                onClose = { navController.popBackStack() },
                onSelectionner = { uid, code ->
                    navController.navigate(Routes.conversation(uid, code)) {
                        popUpTo(Routes.DEPECHES)
                    }
                }
            )
        }
        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(
                androidx.navigation.navArgument("uid") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("code") { type = androidx.navigation.NavType.StringType }
            )
        ) { entry ->
            val uid = URLDecoder.decode(entry.arguments?.getString("uid").orEmpty(), "UTF-8")
            val code = URLDecoder.decode(entry.arguments?.getString("code").orEmpty(), "UTF-8")
            ConversationScreen(
                otherUid = uid,
                otherCode = code,
                onClose = { navController.popBackStack() }
            )
        }
    }
}
