package com.okamilang.mysteria.urgent

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.okamilang.mysteria.MainActivity
import com.okamilang.mysteria.ui.theme.MysteriaTheme

/**
 * Activité plein-écran lancée à la réception d'une dépêche urgente.
 * Doit pouvoir s'afficher au-dessus de l'écran de verrouillage et
 * réveiller le téléphone (showWhenLocked + turnScreenOn dans le manifest).
 */
class UrgentTransmissionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FROM_CODE = "fromCode"
        const val EXTRA_FROM_UID = "fromUid"
        const val EXTRA_BODY = "body"
        const val EXTRA_MESSAGE_ID = "messageId"
    }

    private var vibrator: Vibrator? = null
    private var ringtone: android.media.Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        val fromCode = intent.getStringExtra(EXTRA_FROM_CODE) ?: "Agent inconnu"
        val fromUid = intent.getStringExtra(EXTRA_FROM_UID).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()

        startVibration()
        startSound()

        setContent {
            MysteriaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UrgentTransmissionScreen(
                        fromCode = fromCode,
                        body = body,
                        onRepondre = {
                            stopAlerte()
                            ouvrirConversation(fromUid, fromCode)
                            finishAndRemoveTask()
                        },
                        onDifferer = {
                            stopAlerte()
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        stopAlerte()
        super.onDestroy()
    }

    private fun ouvrirConversation(otherUid: String, otherCode: String) {
        if (otherUid.isBlank()) return
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OUVRIR_CONVERSATION_UID, otherUid)
            putExtra(MainActivity.EXTRA_OUVRIR_CONVERSATION_CODE, otherCode)
        }
        startActivity(intent)
    }

    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib

        // Pattern : pause 0ms, vibre 800ms, pause 400ms — répété indéfiniment.
        val timings = longArrayOf(0, 800, 400)
        val amplitudes = intArrayOf(0, 255, 0)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
        vib.vibrate(effect)
    }

    private fun startSound() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri).also {
                it.audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    it.isLooping = true
                }
                it.play()
            }
        }
    }

    private fun stopAlerte() {
        vibrator?.cancel()
        vibrator = null
        runCatching { ringtone?.stop() }
        ringtone = null
    }
}
