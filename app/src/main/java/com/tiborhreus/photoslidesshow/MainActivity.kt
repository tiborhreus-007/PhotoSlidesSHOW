package com.tiborhreus.photoslidesshow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tiborhreus.photoslidesshow.ui.screens.HomeScreen
import com.tiborhreus.photoslidesshow.ui.screens.MusicScreen
import com.tiborhreus.photoslidesshow.ui.screens.SlideshowScreen
import com.tiborhreus.photoslidesshow.ui.screens.SourcesScreen
import com.tiborhreus.photoslidesshow.ui.theme.PhotoSlidesTheme

private enum class AppScreen {
    HOME,
    SOURCES,
    MUSIC,
    SLIDESHOW,
}

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhotoSlidesTheme {
                val settings by viewModel.settings.collectAsState()
                val sources by viewModel.sources.collectAsState()
                val music by viewModel.music.collectAsState()
                val albums by viewModel.albums.collectAsState()
                val playlist by viewModel.playlist.collectAsState()

                var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }

                BackHandler(enabled = screen != AppScreen.HOME) {
                    if (screen == AppScreen.SLIDESHOW) {
                        viewModel.stopSlideshow()
                    }
                    screen = AppScreen.HOME
                }

                when (screen) {
                    AppScreen.HOME -> HomeScreen(
                        settings = settings,
                        sources = sources,
                        music = music,
                        onUpdateSettings = viewModel::updateSettings,
                        onResetSettings = viewModel::resetSettings,
                        onOpenSources = { screen = AppScreen.SOURCES },
                        onOpenMusic = { screen = AppScreen.MUSIC },
                        onStartSlideshow = {
                            viewModel.startSlideshow()
                            screen = AppScreen.SLIDESHOW
                        },
                    )

                    AppScreen.SOURCES -> SourcesScreen(
                        sources = sources,
                        albums = albums,
                        onAddSource = viewModel::addSource,
                        onRemoveSource = viewModel::removeSource,
                        onLoadAlbums = viewModel::loadAlbums,
                        onBack = { screen = AppScreen.HOME },
                    )

                    AppScreen.MUSIC -> MusicScreen(
                        music = music,
                        onUpdateMusic = viewModel::updateMusic,
                        onBack = { screen = AppScreen.HOME },
                    )

                    AppScreen.SLIDESHOW -> SlideshowScreen(
                        playlistState = playlist,
                        settings = settings,
                        music = music,
                        onReshuffle = viewModel::reshuffle,
                        onExit = {
                            viewModel.stopSlideshow()
                            screen = AppScreen.HOME
                        },
                    )
                }
            }
        }
    }
}
