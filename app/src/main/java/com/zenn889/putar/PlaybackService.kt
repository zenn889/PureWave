package com.zenn889.putar

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Service pemutar (Media3/ExoPlayer). Menyediakan:
 * - pemutaran tetap jalan saat app di latar belakang,
 * - kontrol di notifikasi & lock screen,
 * - audio focus + "becoming noisy" (headset dicabut -> pause).
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        // notifikasi kontrol memakai provider default media3 (ikon internal)
        val player = ExoPlayer.Builder(this)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    /* handleAudioFocus = */ true
                )
                setHandleAudioBecomingNoisy(true)
                // tempel equalizer/bass ke sesi audio saat tersedia
                addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        if (events.containsAny(
                                Player.EVENT_AUDIO_SESSION_ID,
                                Player.EVENT_PLAYBACK_STATE_CHANGED
                            )
                        ) {
                            val sid = (player as? ExoPlayer)?.audioSessionId
                            if (sid != null && sid > 0) {
                                AudioFx.attach(applicationContext, sid)
                            }
                        }
                    }
                })
            }
        // kunci sesi audio dengan ID tetap agar efek equalizer selalu menempel
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val fixedSession = runCatching { audioManager.generateAudioSessionId() }.getOrDefault(0)
        if (fixedSession != 0) {
            runCatching { player.setAudioSessionId(fixedSession) }
            AudioFx.attach(this, fixedSession)
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    /** Panggil dari UI: tempel ulang efek ke sesi yang sedang berjalan. */
    fun refreshFx() {
        val player = mediaSession?.player
        if (player != null) {
            val sid = (player as? ExoPlayer)?.audioSessionId
            if (sid != null && sid > 0) {
                AudioFx.attach(applicationContext, sid)
                return
            }
        }
        AudioFx.applyCurrent(applicationContext)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        AudioFx.release()
        instance = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: PlaybackService? = null
        fun current(): PlaybackService? = instance
    }
}
