package com.okamilang.mysteria

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.okamilang.mysteria.messaging.FcmTokens
import com.okamilang.mysteria.messaging.Notifs
import com.okamilang.mysteria.ui.MysteriaApp
import com.okamilang.mysteria.ui.OuvertureConversation
import com.okamilang.mysteria.ui.theme.BrunTresSombre
import com.okamilang.mysteria.ui.theme.MysteriaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OUVRIR_CONVERSATION_UID = "ouvrirConversationUid"
        const val EXTRA_OUVRIR_CONVERSATION_CODE = "ouvrirConversationCode"
    }

    private var ouvertureConversation by mutableStateOf<OuvertureConversation?>(null)

    private val demandePermissionNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* on ne bloque pas si refusé : l'app reste utilisable sans notifs */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Notifs.ensureChannels(this)
        demanderPermissionNotifSiBesoin()
        observerAuthEtEnregistrerJeton()
        lireExtraOuvertureConversation(intent)

        setContent {
            MysteriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrunTresSombre
                ) {
                    MysteriaApp(
                        ouvertureConversation = ouvertureConversation,
                        onOuvertureConsommee = { ouvertureConversation = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lireExtraOuvertureConversation(intent)
    }

    private fun lireExtraOuvertureConversation(intent: Intent?) {
        val uid = intent?.getStringExtra(EXTRA_OUVRIR_CONVERSATION_UID).orEmpty()
        val code = intent?.getStringExtra(EXTRA_OUVRIR_CONVERSATION_CODE).orEmpty()
        if (uid.isNotBlank() && code.isNotBlank()) {
            ouvertureConversation = OuvertureConversation(uid, code)
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
        FirebaseAuth.getInstance().addAuthStateListener { fa ->
            if (fa.currentUser != null) {
                lifecycleScope.launch { FcmTokens.ensureRegistered() }
            }
        }
    }
}
