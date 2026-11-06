package com.tiborhreus.photoslidesshow.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tiborhreus.photoslidesshow.data.MusicCatalog
import com.tiborhreus.photoslidesshow.data.MusicConfig
import com.tiborhreus.photoslidesshow.data.MusicMode
import com.tiborhreus.photoslidesshow.data.MusicTrack
import com.tiborhreus.photoslidesshow.data.PhotoLoader
import com.tiborhreus.photoslidesshow.ui.components.AdaptiveColumns
import com.tiborhreus.photoslidesshow.ui.components.BorderedButton
import com.tiborhreus.photoslidesshow.ui.components.BorderedIconButton
import com.tiborhreus.photoslidesshow.ui.components.ChoiceRow
import com.tiborhreus.photoslidesshow.ui.components.ScreenScaffold
import com.tiborhreus.photoslidesshow.ui.components.SectionCard
import com.tiborhreus.photoslidesshow.ui.components.SettingLabel
import com.tiborhreus.photoslidesshow.ui.components.SettingSlider
import com.tiborhreus.photoslidesshow.ui.components.SettingSwitch
import com.tiborhreus.photoslidesshow.ui.components.isAppInstalled
import com.tiborhreus.photoslidesshow.ui.components.launchAppOrStore
import com.tiborhreus.photoslidesshow.ui.components.openUrl
import com.tiborhreus.photoslidesshow.ui.theme.AppColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicScreen(
    music: MusicConfig,
    onUpdateMusic: ((MusicConfig) -> MusicConfig) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val added = uris.map { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val title = PhotoLoader.queryDisplayName(context, uri)
                    ?: uri.lastPathSegment
                    ?: "Hudobná skladba"
                MusicTrack(uri = uri.toString(), title = title)
            }
            onUpdateMusic { current ->
                current.copy(
                    mode = MusicMode.LOCAL,
                    tracks = (current.tracks + added).distinctBy { it.uri },
                )
            }
        }
    }

    var customRadioName by remember(music.radioName) { mutableStateOf(music.radioName) }
    var customRadioUrl by remember(music.radioUrl) { mutableStateOf(music.radioUrl) }

    ScreenScaffold(title = "Hudba k prezentácii", onBack = onBack) {
        // Hlavný výber režimu a hlasitosti
        SectionCard(title = "Režim hudobného podkladu", icon = Icons.Filled.MusicNote) {
            ChoiceRow(
                options = MusicMode.entries,
                selected = music.mode,
                onSelect = { m -> onUpdateMusic { it.copy(mode = m) } },
            )

            SettingSlider(
                title = "Hlasitosť vstavanej hudby",
                valueText = "${(music.volume * 100).roundToInt()} %",
                value = music.volume,
                range = 0.05f..1f,
                onChange = { v -> onUpdateMusic { it.copy(volume = v) } },
            )

            SettingSwitch(
                title = "Plynulý nábeh hlasitosti (Fade-In)",
                subtitle = "Pri spustení prezentácie sa hudba postupne zosilní",
                checked = music.fadeIn,
                onChange = { v -> onUpdateMusic { it.copy(fadeIn = v) } },
            )
        }

        AdaptiveColumns(
            left = {
                // 1. Lokálne audio súbory
                SectionCard(title = "Lokálne skladby v zariadení", icon = Icons.Filled.AudioFile) {
                    Text(
                        "Vyberte MP3, FLAC, AAC, WAV alebo OGG súbory z úložiska telefónu/tabletu alebo zo stiahnutých voľne dostupných knižníc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BorderedButton(
                            text = "Pridať audio súbory",
                            icon = Icons.Filled.LibraryMusic,
                            primary = music.mode == MusicMode.LOCAL,
                            modifier = Modifier.weight(1f),
                            onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        )
                        if (music.tracks.isNotEmpty()) {
                            BorderedButton(
                                text = "Vyčistiť",
                                onClick = { onUpdateMusic { it.copy(tracks = emptyList()) } },
                            )
                        }
                    }

                    SettingSwitch(
                        title = "Náhodné poradie skladieb",
                        checked = music.shuffle,
                        onChange = { v -> onUpdateMusic { it.copy(shuffle = v) } },
                    )

                    if (music.tracks.isEmpty()) {
                        Text(
                            "Zatiaľ nie sú vybrané žiadne lokálne skladby.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextMuted,
                        )
                    } else {
                        music.tracks.forEach { track ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppColors.SurfaceHigh,
                                border = BorderStroke(1.dp, AppColors.BorderDim),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        track.title,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    BorderedIconButton(
                                        icon = Icons.Filled.DeleteOutline,
                                        contentDescription = "Odstrániť skladbu",
                                        danger = true,
                                        size = 38.dp,
                                        onClick = {
                                            onUpdateMusic { cur ->
                                                cur.copy(tracks = cur.tracks.filterNot { it.uri == track.uri })
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Voľne dostupné zdroje hudby na stiahnutie
                SectionCard(title = "Voľne dostupná hudba (Royalty-Free)", icon = Icons.Filled.Public) {
                    Text(
                        "Bezplatné hudobné knižnice, z ktorých si môžete jedným kliknutím stiahnuť skladby do zariadenia a pridať ich k prezentácii:",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                    MusicCatalog.freeSites.forEach { site ->
                        BorderedButton(
                            text = site.name,
                            subtitle = site.description,
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { context.openUrl(site.url) },
                        )
                    }
                }
            },
            right = {
                // 3. Voľne dostupné internetové rádiá
                SectionCard(title = "Internetové rádiá bez reklám", icon = Icons.Filled.Radio) {
                    Text(
                        "Priame streamy ambientnej a relaxačnej hudby prehrávané priamo v aplikácii počas prezentácie:",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )

                    MusicCatalog.radios.forEach { station ->
                        val isSelected = music.mode == MusicMode.RADIO && music.radioUrl == station.url
                        BorderedButton(
                            text = station.name,
                            subtitle = station.genre,
                            icon = Icons.Filled.Radio,
                            primary = isSelected,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                customRadioName = station.name
                                customRadioUrl = station.url
                                onUpdateMusic {
                                    it.copy(
                                        mode = MusicMode.RADIO,
                                        radioName = station.name,
                                        radioUrl = station.url,
                                    )
                                }
                            },
                        )
                    }

                    SettingLabel("Vlastný stream (MP3 / AAC / Icecast URL)")
                    OutlinedTextField(
                        value = customRadioName,
                        onValueChange = { customRadioName = it },
                        label = { Text("Názov stanice") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Accent,
                            unfocusedBorderColor = AppColors.Border,
                            focusedTextColor = AppColors.Text,
                            unfocusedTextColor = AppColors.Text,
                        ),
                    )
                    OutlinedTextField(
                        value = customRadioUrl,
                        onValueChange = { customRadioUrl = it },
                        label = { Text("URL streamu (https://…)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Accent,
                            unfocusedBorderColor = AppColors.Border,
                            focusedTextColor = AppColors.Text,
                            unfocusedTextColor = AppColors.Text,
                        ),
                    )
                    BorderedButton(
                        text = "Uložiť vlastné rádio",
                        enabled = customRadioUrl.startsWith("http"),
                        onClick = {
                            onUpdateMusic {
                                it.copy(
                                    mode = MusicMode.RADIO,
                                    radioName = customRadioName.ifBlank { "Vlastný stream" },
                                    radioUrl = customRadioUrl.trim(),
                                )
                            }
                        },
                    )
                }

                // 4. Streamovacie služby
                SectionCard(title = "Streamovacie služby", icon = Icons.Filled.Headphones) {
                    Text(
                        "V režime „Streamovacia aplikácia“ PhotoSlides SHOW nepreberá audio fokus, takže hudba zo Spotify, YouTube Music, Apple Music, TIDAL alebo Deezer hrá plynulo na pozadí počas celej prezentácie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MusicCatalog.streamingApps.forEach { app ->
                            val installed = context.isAppInstalled(app.packageName)
                            BorderedButton(
                                text = app.name,
                                subtitle = if (installed) "Otvoriť aplikáciu" else "Nainštalovať",
                                icon = Icons.Filled.VolumeUp,
                                primary = installed && music.mode == MusicMode.EXTERNAL,
                                onClick = {
                                    onUpdateMusic { it.copy(mode = MusicMode.EXTERNAL) }
                                    context.launchAppOrStore(app.packageName)
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}
