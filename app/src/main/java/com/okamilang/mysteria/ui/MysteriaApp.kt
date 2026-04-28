package com.okamilang.mysteria.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

@Composable
fun MysteriaApp() {
    val navController = rememberNavController()
    val user by remember { AuthRepository.authStateFlow() }
        .collectAsState(initial = AuthRepository.currentUser)

    val start = if (user == null) Routes.IDENTIFICATION else Routes.DEPECHES

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
                navArgument("uid") { type = NavType.StringType },
                navArgument("code") { type = NavType.StringType }
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
