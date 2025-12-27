
package com.maxvibe.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.net.URL

data class Song(val url: String, val title: String, val artwork: String?)

class MusicService : MediaSessionService() {

    companion object {
        var player: ExoPlayer? = null
        const val CHANNEL_ID = "music_channel"
        val musicQueue = mutableListOf<Song>()
        var currentIndex = 0
        lateinit var mediaSession: MediaSession
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("ACTION") ?: "NONE"

        when (action) {
            "PLAY" -> {
                val url = intent.getStringExtra("URL") ?: return START_NOT_STICKY
                val title = intent.getStringExtra("TITLE") ?: "Playing"
                val artwork = intent.getStringExtra("ART")
                playSong(Song(url, title, artwork))
            }
            "PAUSE" -> player?.pause()
            "NEXT" -> playNext()
            "PREVIOUS" -> playPrevious()
            "LOAD_PLAYLIST" -> {
                val json = intent.getStringExtra("PLAYLIST_JSON") ?: return START_NOT_STICKY
                loadPlaylist(json)
            }
        }

        return START_STICKY
    }

    private fun playSong(song: Song) {
        player?.setMediaItem(
            MediaItem.Builder()
                .setUri(song.url)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).build())
                .build()
        )
        player?.prepare()
        player?.play()
        startForeground(1, buildNotification(song))
    }

    private fun playNext() {
        if (musicQueue.isEmpty()) return
        currentIndex = (currentIndex + 1) % musicQueue.size
        playSong(musicQueue[currentIndex])
    }

    private fun playPrevious() {
        if (musicQueue.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) musicQueue.size - 1 else currentIndex - 1
        playSong(musicQueue[currentIndex])
    }

    private fun loadPlaylist(jsonArrayStr: String) {
        musicQueue.clear()
        val array = JSONArray(jsonArrayStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            musicQueue.add(Song(obj.getString("url"), obj.getString("title"), obj.optString("art", null)))
        }
        currentIndex = 0
        if (musicQueue.isNotEmpty()) playSong(musicQueue[0])
    }

    private fun buildNotification(song: Song): Notification {
        val bitmap: Bitmap? = song.artwork?.let {
            try { BitmapFactory.decodeStream(URL(it).openStream()) } catch (e: Exception) { null }
        }

        val prevIntent = PendingIntent.getService(this, 0, Intent(this, MusicService::class.java).apply {
            putExtra("ACTION", "PREVIOUS")
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).apply {
            putExtra("ACTION", "PAUSE")
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).apply {
            putExtra("ACTION", "NEXT")
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(bitmap)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", prevIntent))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pauseIntent))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", nextIntent))
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
