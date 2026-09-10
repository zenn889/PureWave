package com.zenn889.putar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.zenn889.putar.ui.fmtMs

/** Widget pemutar: lagu aktif + bar progres + tombol prev/play/next. */
class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = PlayerWidget.views(context)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE, ACTION_NEXT, ACTION_PREV -> {
                val svc = Intent(context, PlaybackService::class.java)
                    .setAction(intent.action)
                runCatching { context.startForegroundService(svc) }
            }
            ACTION_OPEN -> {
                val open = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(open) }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.zenn889.putar.widget.TOGGLE"
        const val ACTION_NEXT = "com.zenn889.putar.widget.NEXT"
        const val ACTION_PREV = "com.zenn889.putar.widget.PREV"
        const val ACTION_OPEN = "com.zenn889.putar.widget.OPEN"

        /**
         * Simpan snapshot (judul, artis, status, posisi) lalu segarkan semua
         * widget yang terpasang. Kalau tidak ada widget, langsung keluar —
         * jadi tidak ada I/O sama sekali untuk pengguna yang tidak memakainya.
         */
        fun push(
            context: Context,
            title: String,
            artist: String,
            playing: Boolean,
            positionMs: Long = 0L,
            durationMs: Long = 0L
        ) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, PlayerWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            PlayerWidget.push(context, title, artist, playing, positionMs, durationMs)
            val views = PlayerWidget.views(context)
            ids.forEach { mgr.updateAppWidget(it, views) }
        }
    }
}

/** Snapshot kondisi lagu + pembuat RemoteViews. */
object PlayerWidget {
    private const val PREFS = "putar_widget"

    /** Bar progres RemoteViews bekerja pada skala bilangan bulat. */
    private const val PROGRESS_MAX = 1000

    fun push(
        context: Context,
        title: String,
        artist: String,
        playing: Boolean,
        positionMs: Long,
        durationMs: Long
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("title", title)
            .putString("artist", artist)
            .putBoolean("playing", playing)
            .putLong("position", positionMs)
            .putLong("duration", durationMs)
            .apply()
    }

    fun views(context: Context): RemoteViews {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val title = p.getString("title", "PureWave").orEmpty()
        val artist = p.getString("artist", "Pemutar offline").orEmpty()
        val playing = p.getBoolean("playing", false)
        val position = p.getLong("position", 0L)
        val duration = p.getLong("duration", 0L)

        val rv = RemoteViews(context.packageName, R.layout.widget_player)
        rv.setTextViewText(R.id.w_title, title)
        rv.setTextViewText(R.id.w_artist, artist)
        rv.setImageViewResource(
            R.id.w_play,
            if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )

        // progres lagu (temuan audit #12)
        val known = duration > 0L
        val shown = if (known) position.coerceIn(0L, duration) else 0L
        val permille = if (known) ((shown * PROGRESS_MAX) / duration).toInt() else 0
        rv.setProgressBar(R.id.w_progress, PROGRESS_MAX, permille, false)
        rv.setTextViewText(
            R.id.w_time,
            if (known) "${fmtMs(shown)} / ${fmtMs(duration)}" else ""
        )

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        rv.setOnClickPendingIntent(
            R.id.w_prev,
            PendingIntent.getBroadcast(
                context, 1,
                Intent(context, PlayerWidgetProvider::class.java)
                    .setAction(PlayerWidgetProvider.ACTION_PREV),
                flags
            )
        )
        rv.setOnClickPendingIntent(
            R.id.w_play,
            PendingIntent.getBroadcast(
                context, 2,
                Intent(context, PlayerWidgetProvider::class.java)
                    .setAction(PlayerWidgetProvider.ACTION_TOGGLE),
                flags
            )
        )
        rv.setOnClickPendingIntent(
            R.id.w_next,
            PendingIntent.getBroadcast(
                context, 3,
                Intent(context, PlayerWidgetProvider::class.java)
                    .setAction(PlayerWidgetProvider.ACTION_NEXT),
                flags
            )
        )
        rv.setOnClickPendingIntent(
            R.id.w_root,
            PendingIntent.getActivity(
                context, 4,
                Intent(context, MainActivity::class.java),
                flags
            )
        )
        return rv
    }
}
