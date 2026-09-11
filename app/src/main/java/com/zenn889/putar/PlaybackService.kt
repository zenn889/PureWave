package com.zenn889.putar

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zenn889.putar.data.VolumeNorm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service pemutar (Media3/ExoPlayer). Menyediakan:
 * - pemutaran tetap jalan saat app di latar belakang,
 * - kontrol di notifikasi & lock screen,
 * - audio focus + "becoming noisy" (headset dicabut -> pause),
 * - dorongan info + progres lagu ke widget home screen.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Berjalan selama service hidup; dipakai untuk detak progres widget. */
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
                        if (events.containsAny(
                                Player.EVENT_PLAYBACK_STATE_CHANGED,
                                Player.EVENT_IS_PLAYING_CHANGED,
                                Player.EVENT_MEDIA_METADATA_CHANGED,
                                Player.EVENT_MEDIA_ITEM_TRANSITION
                            )
                        ) {
                            pushWidget(player)
                        }
                        if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                            applyVolumeNorm(player)
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

        // Progres di widget tidak punya event — ia berjalan terus. Jadi kita
        // dorong ~1x/detik selama lagu berputar. `pushWidget` langsung keluar
        // kalau widget belum pernah dipasang, jadi tanpa widget = nol biaya.
        widgetScope.launch {
            while (isActive) {
                val p = mediaSession?.player
                if (p != null && p.isPlaying) pushWidget(p)
                delay(1_000)
            }
        }
    }

    /** Kirim judul, artis, status, dan posisi lagu ke widget. */
    private fun pushWidget(player: Player) {
        val meta = player.mediaMetadata
        PlayerWidgetProvider.push(
            applicationContext,
            meta.title?.toString() ?: "PureWave",
            meta.artist?.toString().orEmpty().ifEmpty { "Pemutar offline" },
            player.isPlaying,
            player.currentPosition.coerceAtLeast(0L),
            player.duration.coerceAtLeast(0L)
        )
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

    /** Panggil dari UI: hitung ulang gain normalisasi (setelah setelan diubah). */
    fun refreshVolumeNorm() {
        val player = mediaSession?.player ?: return
        if (player.mediaItemCount == 0) return
        applyVolumeNorm(player)
    }

    /**
     * Terapkan normalisasi volume untuk lagu yang sedang diputar. Pembacaan tag
     * dilakukan di IO thread karena menyentuh file; volume diset kembali di
     * main thread. Kalau fitur mati (atau lagu tanpa tag ReplayGain), gain 1.0.
     */
    private fun applyVolumeNorm(player: Player) {
        val item = player.currentMediaItem ?: return
        val uri = item.mediaId
        if (!VolumeNorm.enabled(applicationContext)) {
            VolumeNorm.setBaseGain(1f)
            player.volume = 1f
            return
        }
        widgetScope.launch {
            val gain = withContext(Dispatchers.IO) {
                VolumeNorm.gainFor(
                    applicationContext, uri,
                    VolumeNorm.filePathFor(applicationContext, uri)
                )
            }
            VolumeNorm.setBaseGain(gain)
            player.volume = gain
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PlayerWidgetProvider.ACTION_TOGGLE -> mediaSession?.player?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
            PlayerWidgetProvider.ACTION_NEXT -> mediaSession?.player?.let {
                runCatching { it.seekToNextMediaItem() }
            }
            PlayerWidgetProvider.ACTION_PREV -> mediaSession?.player?.let {
                runCatching { it.seekToPreviousMediaItem() }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        widgetScope.cancel()
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
