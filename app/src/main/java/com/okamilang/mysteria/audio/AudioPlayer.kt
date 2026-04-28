package com.okamilang.mysteria.audio

import android.media.MediaPlayer
import android.util.Log

/**
 * Lecteur global mono : un seul cylindre se joue à la fois.
 */
object AudioPlayer {

    private val tag = "Mysteria/Player"
    private var current: MediaPlayer? = null
    private var currentUrl: String? = null
    private val listeners = mutableMapOf<String, () -> Unit>()

    fun isPlaying(url: String): Boolean = currentUrl == url && current?.isPlaying == true

    /** Joue un cylindre depuis une URL. Le callback `onCompletion` est invoqué à la fin. */
    fun play(url: String, onCompletion: () -> Unit) {
        stop()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    listeners.remove(url)?.invoke()
                    if (currentUrl == url) {
                        current = null
                        currentUrl = null
                    }
                    it.release()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(tag, "Erreur lecture $what / $extra")
                    listeners.remove(url)?.invoke()
                    true
                }
                prepareAsync()
            }
            current = mp
            currentUrl = url
            listeners[url] = onCompletion
        } catch (e: Exception) {
            Log.w(tag, "Échec lecture", e)
            onCompletion()
        }
    }

    fun stop() {
        try { current?.stop() } catch (_: Exception) {}
        try { current?.release() } catch (_: Exception) {}
        val url = currentUrl
        current = null
        currentUrl = null
        if (url != null) listeners.remove(url)?.invoke()
    }
}
