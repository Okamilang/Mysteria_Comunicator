package com.okamilang.mysteria.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.okamilang.mysteria.data.AuthRepository
import com.okamilang.mysteria.ui.screens.DepechesScreen
import com.okamilang.mysteria.ui.screens.IdentificationScreen
import com.okamilang.mysteria.ui.screens.NouvelleDepecheScreen

object Routes {
    const val IDENTIFICATION = "identification"
    const val DEPECHES = "depeches"
    const val NOUVELLE_DEPECHE = "nouvelle_depeche"
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
                onNouvelleDepeche = {
                    navController.navigate(Routes.NOUVELLE_DEPECHE)
                }
            )
        }
        composable(Routes.NOUVELLE_DEPECHE) {
            NouvelleDepecheScreen(onClose = { navController.popBackStack() })
        }
    }
}
