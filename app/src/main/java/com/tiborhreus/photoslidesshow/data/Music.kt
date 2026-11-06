package com.tiborhreus.photoslidesshow.data

enum class MusicMode(override val label: String) : Labeled {
    NONE("Bez hudby"),
    LOCAL("Lokálne súbory"),
    RADIO("Internetové rádio"),
    EXTERNAL("Streamovacia aplikácia"),
}

data class MusicTrack(val uri: String, val title: String)

data class MusicConfig(
    val mode: MusicMode = MusicMode.NONE,
    val tracks: List<MusicTrack> = emptyList(),
    val shuffle: Boolean = true,
    val radioName: String = "",
    val radioUrl: String = "",
    val volume: Float = 0.8f,
    val fadeIn: Boolean = true,
)

data class RadioStation(val name: String, val genre: String, val url: String)

data class StreamingApp(val name: String, val packageName: String)

data class FreeMusicSite(val name: String, val description: String, val url: String)

object MusicCatalog {
    /** Voľne dostupné internetové rádiá bez reklám (podporované poslucháčmi). */
    val radios = listOf(
        RadioStation("SomaFM – Groove Salad", "Ambient / downtempo", "https://ice1.somafm.com/groovesalad-128-mp3"),
        RadioStation("SomaFM – Drone Zone", "Atmosférický ambient", "https://ice1.somafm.com/dronezone-128-mp3"),
        RadioStation("SomaFM – Lush", "Jemné vokály, chill", "https://ice1.somafm.com/lush-128-mp3"),
        RadioStation("Radio Paradise – Main Mix", "Eklektický mix", "https://stream.radioparadise.com/mp3-128"),
        RadioStation("Radio Paradise – Mellow", "Pokojný mix", "https://stream.radioparadise.com/mellow-128"),
        RadioStation("Radio Paradise – World/Etc", "World music", "https://stream.radioparadise.com/world-etc-128"),
    )

    val streamingApps = listOf(
        StreamingApp("Spotify", "com.spotify.music"),
        StreamingApp("YouTube Music", "com.google.android.apps.youtube.music"),
        StreamingApp("Apple Music", "com.apple.android.music"),
        StreamingApp("Deezer", "deezer.android.app"),
        StreamingApp("TIDAL", "com.aspiro.tidal"),
        StreamingApp("SoundCloud", "com.soundcloud.android"),
        StreamingApp("Amazon Music", "com.amazon.mp3"),
    )

    val freeSites = listOf(
        FreeMusicSite("Pixabay Music", "Hudba bez licenčných poplatkov", "https://pixabay.com/music/"),
        FreeMusicSite("Free Music Archive", "Creative Commons hudba", "https://freemusicarchive.org/"),
        FreeMusicSite("Incompetech", "Kevin MacLeod – royalty free", "https://incompetech.com/music/royalty-free/"),
        FreeMusicSite("ccMixter", "Remixy pod licenciou CC", "https://ccmixter.org/"),
    )
}
