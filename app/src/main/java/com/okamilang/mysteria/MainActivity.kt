package com.okamilang.mysteria

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.okamilang.mysteria.messaging.FcmTokens
import com.okamilang.mysteria.messaging.Notifs
import com.okamilang.mysteria.ui.MysteriaApp
import com.okamilang.mysteria.ui.theme.BrunTresSombre
import com.okamilang.mysteria.ui.theme.MysteriaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val demandePermissionNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* on ne bloque pas si refusé : l'app reste utilisable sans notifs */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Notifs.ensureChannels(this)
        demanderPermissionNotifSiBesoin()
        observerAuthEtEnregistrerJeton()

        setContent {
            MysteriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrunTresSombre
                ) {
                    MysteriaApp()
                }
            }
        }
    }

    private fun demanderPermissionNotifSiBesoin() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val accordee = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!accordee) {
            demandePermissionNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun observerAuthEtEnregistrerJeton() {
        // Quand l'agent se connecte, on attache son jeton FCM courant à
        // son document `agents/{uid}` pour que la Cloud Function puisse
        // lui envoyer des dépêches.
        FirebaseAuth.getInstance().addAuthStateListener { fa ->
            if (fa.currentUser != null) {
                lifecycleScope.launch { FcmTokens.ensureRegistered() }
            }
        }
    }
}
