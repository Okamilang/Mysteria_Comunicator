package com.okamilang.mysteria.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Wrapper minimal autour de MediaRecorder pour enregistrer un cylindre.
 * Format : MPEG_4 / AAC / mono / 44.1 kHz / 96 kbps — bonne qualité,
 * fichier ~750 Ko/min.
 */
class AudioRecorder(private val context: Context) {

    private val tag = "Mysteria/Recorder"
    private var recorder: MediaRecorder? = null
    private var fichier: File? = null
    private var debutMs: Long = 0L

    fun start(): File? = try {
        val cible = File.createTempFile("cylindre_", ".m4a", context.cacheDir)
        @Suppress("DEPRECATION")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else MediaRecorder()
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(96_000)
            setAudioChannels(1)
            setOutputFile(cible.absolutePath)
            prepare()
            start()
        }
        recorder = rec
        fichier = cible
        debutMs = System.currentTimeMillis()
        cible
    } catch (e: Exception) {
        Log.e(tag, "Échec démarrage enregistrement", e)
        nettoyer()
        null
    }

    /** Arrête et renvoie le fichier + durée en ms. Renvoie null si rien d'enregistré. */
    fun stop(): Pair<File, Long>? {
        val rec = recorder
        val f = fichier
        val durationMs = if (debutMs > 0) System.currentTimeMillis() - debutMs else 0L
        recorder = null
        fichier = null
        debutMs = 0L
        return try {
            rec?.apply { stop(); release() }
            if (f != null && f.exists() && f.length() > 0 && durationMs >= 500)
                f to durationMs else {
                    f?.delete()
                    null
                }
        } catch (e: Exception) {
            Log.w(tag, "Échec arrêt enregistrement", e)
            f?.delete()
            null
        }
    }

    fun cancel() {
        nettoyer()
    }

    private fun nettoyer() {
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        fichier?.delete()
        fichier = null
        debutMs = 0L
    }
}
