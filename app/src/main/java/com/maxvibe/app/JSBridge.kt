
package com.maxvibe.app

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import org.json.JSONArray

class JSBridge(private val context: Context) {

    @JavascriptInterface
    fun play(url: String, title: String, artwork: String?) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra("URL", url)
        intent.putExtra("TITLE", title)
        intent.putExtra("ART", artwork)
        intent.putExtra("ACTION", "PLAY")
        context.startService(intent)
    }

    @JavascriptInterface
    fun pause() {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra("ACTION", "PAUSE")
        context.startService(intent)
    }

    @JavascriptInterface
    fun next() {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra("ACTION", "NEXT")
        context.startService(intent)
    }

    @JavascriptInterface
    fun previous() {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra("ACTION", "PREVIOUS")
        context.startService(intent)
    }

    @JavascriptInterface
    fun loadPlaylist(jsonArrayStr: String) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra("ACTION", "LOAD_PLAYLIST")
        intent.putExtra("PLAYLIST_JSON", jsonArrayStr)
        context.startService(intent)
    }
}
