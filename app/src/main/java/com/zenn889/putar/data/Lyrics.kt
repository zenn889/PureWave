package com.zenn889.putar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Lirik berbasis file .lrc di sebelah lagu (offline). */
object LyricsLoader {

    data class Line(val timeMs: Long, val text: String)

    /** Cari file .lrc dengan nama sama seperti lagu (beberapa variasi kapital). */
    private fun lrcFile(filePath: String?): File? {
        if (filePath.isNullOrEmpty()) return null
        val f = File(filePath)
        val base = f.absolutePath.substringBeforeLast('.', f.absolutePath)
        val candidates = listOf("$base.lrc", "$base.LRC", "$base.Lrc")
        return candidates.map { File(it) }.firstOrNull { it.isFile && it.canRead() }
    }

    suspend fun load(context: Context, trackFileName: String?, title: String, folder: String?): List<Line> =
        withContext(Dispatchers.IO) {
            val file = lrcFile(trackFileName)
            val text = when {
                file != null -> runCatching { file.readText(Charsets.UTF_8) }
                    .recoverCatching { file.readText(Charsets.ISO_8859_1) }
                    .getOrNull()
                else -> null
            } ?: return@withContext emptyList()
            parse(text)
        }

    /** Parse format LRC: [mm:ss.xx] teks (boleh beberapa tag per baris). */
    fun parse(text: String): List<Line> {
        val out = ArrayList<Line>()
        val regex = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
        text.lineSequence().forEach { raw ->
            val matches = regex.findAll(raw).toList()
            if (matches.isEmpty()) return@forEach
            val content = raw.substring(matches.last().range.last + 1).trim()
            matches.forEach { m ->
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = m.groupValues[3]
                val ms = when {
                    fracRaw.isEmpty() -> 0L
                    fracRaw.length == 1 -> fracRaw.toLong() * 100L
                    fracRaw.length == 2 -> fracRaw.toLong() * 10L
                    else -> fracRaw.substring(0, 3).toLong()
                }
                out.add(Line(min * 60_000L + sec * 1000L + ms, content))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /** File subtitle (.srt / .vtt) dengan nama sama seperti video. */
    fun subtitleFile(filePath: String?): File? {
        if (filePath.isNullOrEmpty()) return null
        val f = File(filePath)
        val base = f.absolutePath.substringBeforeLast('.', f.absolutePath)
        val candidates = listOf(
            "$base.srt", "$base.SRT", "$base.vtt", "$base.VTT", "$base.ttml"
        )
        return candidates.map { File(it) }.firstOrNull { it.isFile && it.canRead() }
    }
}
