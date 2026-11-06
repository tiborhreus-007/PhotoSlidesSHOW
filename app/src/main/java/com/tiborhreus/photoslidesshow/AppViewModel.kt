package com.tiborhreus.photoslidesshow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiborhreus.photoslidesshow.data.AlbumInfo
import com.tiborhreus.photoslidesshow.data.MusicConfig
import com.tiborhreus.photoslidesshow.data.PhotoLoader
import com.tiborhreus.photoslidesshow.data.PhotoSource
import com.tiborhreus.photoslidesshow.data.PlaylistBuilder
import com.tiborhreus.photoslidesshow.data.PlaylistState
import com.tiborhreus.photoslidesshow.data.PrefsStore
import com.tiborhreus.photoslidesshow.data.SlideshowSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PrefsStore(app)

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<SlideshowSettings> = _settings.asStateFlow()

    private val _sources = MutableStateFlow(store.loadSources())
    val sources: StateFlow<List<PhotoSource>> = _sources.asStateFlow()

    private val _music = MutableStateFlow(store.loadMusic())
    val music: StateFlow<MusicConfig> = _music.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumInfo>?>(null)
    val albums: StateFlow<List<AlbumInfo>?> = _albums.asStateFlow()

    private val _playlist = MutableStateFlow<PlaylistState>(PlaylistState.Idle)
    val playlist: StateFlow<PlaylistState> = _playlist.asStateFlow()

    private var playlistJob: Job? = null

    // ---------- Nastavenia ----------

    fun updateSettings(transform: (SlideshowSettings) -> SlideshowSettings) {
        _settings.update(transform)
        store.saveSettings(_settings.value)
    }

    fun resetSettings() = updateSettings { SlideshowSettings() }

    // ---------- Zdroje ----------

    fun addSource(source: PhotoSource) {
        _sources.update { list -> if (list.any { it.key == source.key }) list else list + source }
        store.saveSources(_sources.value)
    }

    fun removeSource(source: PhotoSource) {
        _sources.update { list -> list.filterNot { it.key == source.key } }
        store.saveSources(_sources.value)
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _albums.value = null
            _albums.value = runCatching { PhotoLoader.loadAlbums(getApplication()) }
                .getOrDefault(emptyList())
        }
    }

    // ---------- Hudba ----------

    fun updateMusic(transform: (MusicConfig) -> MusicConfig) {
        _music.update(transform)
        store.saveMusic(_music.value)
    }

    // ---------- Prezentácia ----------

    fun startSlideshow() {
        playlistJob?.cancel()
        _playlist.value = PlaylistState.Loading
        playlistJob = viewModelScope.launch {
            val all = runCatching { PhotoLoader.loadPhotos(getApplication(), _sources.value) }
                .getOrElse {
                    _playlist.value = PlaylistState.Error(it.message ?: "Neznáma chyba")
                    return@launch
                }
            val built = PlaylistBuilder.build(all, _settings.value)
            _playlist.value = if (built.isEmpty()) {
                PlaylistState.Empty(all.size)
            } else {
                PlaylistState.Ready(built, System.nanoTime())
            }
        }
    }

    /** Pri náhodnom poradí sa po každom kole fotky znova zamiešajú. */
    fun reshuffle() {
        val current = _playlist.value as? PlaylistState.Ready ?: return
        _playlist.value = PlaylistState.Ready(
            PlaylistBuilder.sort(current.photos, _settings.value.sortOrder),
            System.nanoTime(),
        )
    }

    fun stopSlideshow() {
        playlistJob?.cancel()
        _playlist.value = PlaylistState.Idle
    }
}
