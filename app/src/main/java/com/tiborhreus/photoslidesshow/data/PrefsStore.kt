package com.tiborhreus.photoslidesshow.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Jednoduché trvalé uloženie nastavení, zdrojov a hudby do SharedPreferences (JSON). */
class PrefsStore(context: Context) {
    private val prefs = context.getSharedPreferences("photoslides", Context.MODE_PRIVATE)

    // ---------- Nastavenia ----------

    fun loadSettings(): SlideshowSettings = runCatching {
        prefs.getString(KEY_SETTINGS, null)?.let { decodeSettings(JSONObject(it)) }
    }.getOrNull() ?: SlideshowSettings()

    fun saveSettings(s: SlideshowSettings) {
        prefs.edit().putString(KEY_SETTINGS, encodeSettings(s).toString()).apply()
    }

    private fun encodeSettings(s: SlideshowSettings) = JSONObject().apply {
        put("displaySeconds", s.displaySeconds)
        put("transitionMs", s.transitionMs)
        put("transition", s.transition.name)
        put("kenBurns", s.kenBurns)
        put("sortOrder", s.sortOrder.name)
        put("orientationFilter", s.orientationFilter.name)
        put("periodFilter", s.periodFilter.name)
        put("scaleMode", s.scaleMode.name)
        put("background", s.background.name)
        put("pairPortraits", s.pairPortraits)
        put("screenOrientation", s.screenOrientation.name)
        put("loop", s.loop)
        put("showInfo", s.showInfo)
        put("showClock", s.showClock)
        put("showProgress", s.showProgress)
        put("sleepTimer", s.sleepTimer.name)
    }

    private fun decodeSettings(o: JSONObject): SlideshowSettings {
        val d = SlideshowSettings()
        return SlideshowSettings(
            displaySeconds = o.optInt("displaySeconds", d.displaySeconds),
            transitionMs = o.optInt("transitionMs", d.transitionMs),
            transition = o.optString("transition").toEnum(d.transition),
            kenBurns = o.optBoolean("kenBurns", d.kenBurns),
            sortOrder = o.optString("sortOrder").toEnum(d.sortOrder),
            orientationFilter = o.optString("orientationFilter").toEnum(d.orientationFilter),
            periodFilter = o.optString("periodFilter").toEnum(d.periodFilter),
            scaleMode = o.optString("scaleMode").toEnum(d.scaleMode),
            background = o.optString("background").toEnum(d.background),
            pairPortraits = o.optBoolean("pairPortraits", d.pairPortraits),
            screenOrientation = o.optString("screenOrientation").toEnum(d.screenOrientation),
            loop = o.optBoolean("loop", d.loop),
            showInfo = o.optBoolean("showInfo", d.showInfo),
            showClock = o.optBoolean("showClock", d.showClock),
            showProgress = o.optBoolean("showProgress", d.showProgress),
            sleepTimer = o.optString("sleepTimer").toEnum(d.sleepTimer),
        )
    }

    // ---------- Zdroje fotiek ----------

    fun loadSources(): List<PhotoSource> = runCatching {
        val raw = prefs.getString(KEY_SOURCES, null) ?: return listOf(PhotoSource.Demo)
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i -> decodeSource(arr.getJSONObject(i)) }
    }.getOrDefault(listOf(PhotoSource.Demo))

    fun saveSources(list: List<PhotoSource>) {
        val arr = JSONArray()
        list.forEach { arr.put(encodeSource(it)) }
        prefs.edit().putString(KEY_SOURCES, arr.toString()).apply()
    }

    private fun encodeSource(s: PhotoSource) = JSONObject().apply {
        when (s) {
            is PhotoSource.AllPhotos -> put("type", "all")
            is PhotoSource.Demo -> put("type", "demo")
            is PhotoSource.Album -> {
                put("type", "album"); put("bucketId", s.bucketId); put("name", s.name); put("count", s.count)
            }
            is PhotoSource.Folder -> {
                put("type", "folder"); put("treeUri", s.treeUri); put("name", s.name)
            }
            is PhotoSource.Files -> {
                put("type", "files"); put("name", s.name); put("createdAt", s.createdAt)
                put("uris", JSONArray(s.uris))
            }
        }
    }

    private fun decodeSource(o: JSONObject): PhotoSource? = when (o.optString("type")) {
        "all" -> PhotoSource.AllPhotos
        "demo" -> PhotoSource.Demo
        "album" -> PhotoSource.Album(o.getLong("bucketId"), o.optString("name"), o.optInt("count"))
        "folder" -> PhotoSource.Folder(o.getString("treeUri"), o.optString("name"))
        "files" -> {
            val a = o.getJSONArray("uris")
            PhotoSource.Files(List(a.length()) { a.getString(it) }, o.optString("name"), o.optLong("createdAt"))
        }
        else -> null
    }

    // ---------- Hudba ----------

    fun loadMusic(): MusicConfig = runCatching {
        prefs.getString(KEY_MUSIC, null)?.let { json ->
            val o = JSONObject(json)
            val d = MusicConfig()
            val tracksArr = o.optJSONArray("tracks") ?: JSONArray()
            MusicConfig(
                mode = o.optString("mode").toEnum(d.mode),
                tracks = (0 until tracksArr.length()).map { i ->
                    val t = tracksArr.getJSONObject(i)
                    MusicTrack(t.getString("uri"), t.optString("title"))
                },
                shuffle = o.optBoolean("shuffle", d.shuffle),
                radioName = o.optString("radioName", d.radioName),
                radioUrl = o.optString("radioUrl", d.radioUrl),
                volume = o.optDouble("volume", d.volume.toDouble()).toFloat(),
                fadeIn = o.optBoolean("fadeIn", d.fadeIn),
            )
        }
    }.getOrNull() ?: MusicConfig()

    fun saveMusic(m: MusicConfig) {
        val o = JSONObject().apply {
            put("mode", m.mode.name)
            put("tracks", JSONArray().apply {
                m.tracks.forEach { put(JSONObject().put("uri", it.uri).put("title", it.title)) }
            })
            put("shuffle", m.shuffle)
            put("radioName", m.radioName)
            put("radioUrl", m.radioUrl)
            put("volume", m.volume.toDouble())
            put("fadeIn", m.fadeIn)
        }
        prefs.edit().putString(KEY_MUSIC, o.toString()).apply()
    }

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_SOURCES = "sources"
        const val KEY_MUSIC = "music"
    }
}

inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
