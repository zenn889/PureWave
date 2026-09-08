package com.zenn889.putar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget pemutar: tampilkan lagu aktif + tombol prev/play/next. */
class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, PlayerWidget.views(context, null))
        }
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

        fun push(context: Context, title: String, artist: String, playing: Boolean) {
            PlayerWidget.push(context, title, artist, playing)
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                ids.forEach { mgr.updateAppWidget(it, PlayerWidget.views(context, null)) }
            }
        }
    }
}

/** Snapshot kondisi lagu + pembuat RemoteViews. */
object PlayerWidget {
    private const val PREFS = "putar_widget"

    fun push(context: Context, title: String, artist: String, playing: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("title", title)
            .putString("artist", artist)
            .putBoolean("playing", playing)
            .apply()
    }

    fun views(context: Context, override: Pair<String, String>?): RemoteViews {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val title = override?.first ?: p.getString("title", "PureWave").orEmpty()
        val artist = override?.second ?: p.getString("artist", "Pemutar offline").orEmpty()
        val playing = p.getBoolean("playing", false)

        val rv = RemoteViews(context.packageName, R.layout.widget_player)
        rv.setTextViewText(R.id.w_title, title)
        rv.setTextViewText(R.id.w_artist, artist)
        rv.setImageViewResource(
            R.id.w_play,
            if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
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
