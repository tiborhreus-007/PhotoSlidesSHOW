package com.tiborhreus.photoslidesshow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiborhreus.photoslidesshow.data.BackgroundStyle
import com.tiborhreus.photoslidesshow.data.MusicConfig
import com.tiborhreus.photoslidesshow.data.MusicMode
import com.tiborhreus.photoslidesshow.data.OrientationFilter
import com.tiborhreus.photoslidesshow.data.PeriodFilter
import com.tiborhreus.photoslidesshow.data.PhotoSource
import com.tiborhreus.photoslidesshow.data.ScaleMode
import com.tiborhreus.photoslidesshow.data.ScreenOrientation
import com.tiborhreus.photoslidesshow.data.SleepTimer
import com.tiborhreus.photoslidesshow.data.SlideshowSettings
import com.tiborhreus.photoslidesshow.data.SortOrder
import com.tiborhreus.photoslidesshow.data.TransitionType
import com.tiborhreus.photoslidesshow.data.tracksCount
import com.tiborhreus.photoslidesshow.ui.components.AdaptiveColumns
import com.tiborhreus.photoslidesshow.ui.components.BorderedButton
import com.tiborhreus.photoslidesshow.ui.components.BorderedIconButton
import com.tiborhreus.photoslidesshow.ui.components.ChoiceRow
import com.tiborhreus.photoslidesshow.ui.components.ScreenScaffold
import com.tiborhreus.photoslidesshow.ui.components.SectionCard
import com.tiborhreus.photoslidesshow.ui.components.SettingLabel
import com.tiborhreus.photoslidesshow.ui.components.SettingSlider
import com.tiborhreus.photoslidesshow.ui.components.SettingSwitch
import com.tiborhreus.photoslidesshow.ui.theme.AppColors
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    settings: SlideshowSettings,
    sources: List<PhotoSource>,
    music: MusicConfig,
    onUpdateSettings: ((SlideshowSettings) -> SlideshowSettings) -> Unit,
    onResetSettings: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenMusic: () -> Unit,
    onStartSlideshow: () -> Unit,
) {
    ScreenScaffold(
        title = "PhotoSlides SHOW",
        onBack = null,
        actions = {
            BorderedIconButton(
                icon = Icons.Filled.Refresh,
                contentDescription = "Obnoviť predvolené nastavenia",
                onClick = onResetSettings,
            )
        },
    ) {
        // Hlavný spúšťací panel
        SectionCard(title = "Prezentácia fotiek", icon = Icons.Filled.PlayArrow) {
            val sourceSummary = if (sources.isEmpty()) {
                "Žiadny zdroj – najprv vyberte album, priečinok alebo cloud"
            } else {
                sources.joinToString(" • ") { it.label }
            }
            Text(
                sourceSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (sources.isEmpty()) AppColors.Danger else AppColors.TextMuted,
            )

            BorderedButton(
                text = if (sources.isEmpty()) "Vybrať zdroje fotiek" else "Spustiť prezentáciu",
                subtitle = "${settings.displaySeconds} s na fotku • ${settings.transition.label} • ${settings.sortOrder.label}",
                icon = if (sources.isEmpty()) Icons.Filled.FolderOpen else Icons.Filled.PlayArrow,
                primary = true,
                large = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = if (sources.isEmpty()) onOpenSources else onStartSlideshow,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BorderedButton(
                    text = "Zdroje (${sources.size})",
                    subtitle = "Zariadenie & Cloud",
                    icon = Icons.Filled.FolderOpen,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSources,
                )
                BorderedButton(
                    text = "Hudba",
                    subtitle = musicSummary(music),
                    icon = Icons.Filled.MusicNote,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMusic,
                )
            }
        }

        // Dvojstĺpcové rozloženie na tablete, jednostĺpcové na telefóne
        AdaptiveColumns(
            left = {
                // Časovanie a prechody
                SectionCard(title = "Rýchlosť a prechody", icon = Icons.Filled.Animation) {
                    SettingSlider(
                        title = "Dĺžka zobrazenia fotky",
                        valueText = "${settings.displaySeconds} s",
                        value = settings.displaySeconds.toFloat(),
                        range = 2f..30f,
                        steps = 27,
                        onChange = { v -> onUpdateSettings { it.copy(displaySeconds = v.roundToInt()) } },
                    )

                    SettingSlider(
                        title = "Trvanie prechodu",
                        valueText = "${settings.transitionMs} ms",
                        value = settings.transitionMs.toFloat(),
                        range = 200f..2500f,
                        steps = 22,
                        onChange = { v ->
                            val rounded = (v / 100f).roundToInt() * 100
                            onUpdateSettings { it.copy(transitionMs = rounded) }
                        },
                    )

                    SettingLabel("Štýl prechodu medzi fotkami")
                    ChoiceRow(
                        options = TransitionType.entries,
                        selected = settings.transition,
                        onSelect = { t -> onUpdateSettings { it.copy(transition = t) } },
                    )

                    HorizontalDivider(color = AppColors.BorderDim)

                    SettingSwitch(
                        title = "Jemný pohyb (Ken Burns efekt)",
                        subtitle = "Pomalé plynulé priblíženie a posun počas zobrazenia fotky",
                        checked = settings.kenBurns,
                        onChange = { v -> onUpdateSettings { it.copy(kenBurns = v) } },
                    )
                }

                // Poradie a výberové kritériá
                SectionCard(title = "Poradie a kritériá výberu", icon = Icons.Filled.FilterList) {
                    SettingLabel("Poradie prehrávania")
                    ChoiceRow(
                        options = SortOrder.entries,
                        selected = settings.sortOrder,
                        onSelect = { o -> onUpdateSettings { it.copy(sortOrder = o) } },
                    )

                    Spacer(Modifier.height(4.dp))
                    SettingLabel("Filter podľa orientácie fotky (automatické rozpoznanie)")
                    ChoiceRow(
                        options = OrientationFilter.entries,
                        selected = settings.orientationFilter,
                        onSelect = { f -> onUpdateSettings { it.copy(orientationFilter = f) } },
                    )

                    Spacer(Modifier.height(4.dp))
                    SettingLabel("Časové obdobie fotiek")
                    ChoiceRow(
                        options = PeriodFilter.entries,
                        selected = settings.periodFilter,
                        onSelect = { p -> onUpdateSettings { it.copy(periodFilter = p) } },
                    )
                }
            },
            right = {
                // Otáčanie a prispôsobenie fotiek na výšku / šírku
                SectionCard(title = "Orientácia a zobrazenie fotiek", icon = Icons.Filled.ScreenRotation) {
                    SettingSwitch(
                        title = "Spájať dve fotky na výšku vedľa seba",
                        subtitle = "Pri obrazovke na šírku automaticky zobrazí 2 portrétové fotky naraz",
                        checked = settings.pairPortraits,
                        onChange = { v -> onUpdateSettings { it.copy(pairPortraits = v) } },
                    )

                    SettingLabel("Prispôsobenie fotky obrazovke")
                    ChoiceRow(
                        options = ScaleMode.entries,
                        selected = settings.scaleMode,
                        onSelect = { m -> onUpdateSettings { it.copy(scaleMode = m) } },
                    )

                    SettingLabel("Výplň okrajov pri inom pomere strán")
                    ChoiceRow(
                        options = BackgroundStyle.entries,
                        selected = settings.background,
                        onSelect = { b -> onUpdateSettings { it.copy(background = b) } },
                    )

                    SettingLabel("Otočenie obrazovky počas prezentácie")
                    ChoiceRow(
                        options = ScreenOrientation.entries,
                        selected = settings.screenOrientation,
                        onSelect = { o -> onUpdateSettings { it.copy(screenOrientation = o) } },
                    )
                }

                // Doplnkové funkcie fotorámika
                SectionCard(title = "Doplnky a režim fotorámika", icon = Icons.Filled.Tune) {
                    SettingSwitch(
                        title = "Nekonečná slučka",
                        subtitle = "Po poslednej fotke pokračovať od začiatku (pri náhodnom poradí znova zamieša)",
                        checked = settings.loop,
                        onChange = { v -> onUpdateSettings { it.copy(loop = v) } },
                    )
                    SettingSwitch(
                        title = "Zobraziť informácie o fotke",
                        subtitle = "Názov, dátum vyhotovenia a rozpoznaná orientácia (Na šírku / Na výšku)",
                        checked = settings.showInfo,
                        onChange = { v -> onUpdateSettings { it.copy(showInfo = v) } },
                    )
                    SettingSwitch(
                        title = "Digitálne hodiny v rohu",
                        subtitle = "Vhodné pri použití tabletu alebo telefónu ako stolového fotorámika",
                        checked = settings.showClock,
                        onChange = { v -> onUpdateSettings { it.copy(showClock = v) } },
                    )
                    SettingSwitch(
                        title = "Indikátor priebehu snímky",
                        subtitle = "Tenká lišta odpočítavania času aktuálnej fotky",
                        checked = settings.showProgress,
                        onChange = { v -> onUpdateSettings { it.copy(showProgress = v) } },
                    )

                    SettingLabel("Časovač automatického ukončenia")
                    ChoiceRow(
                        options = SleepTimer.entries,
                        selected = settings.sleepTimer,
                        onSelect = { t -> onUpdateSettings { it.copy(sleepTimer = t) } },
                    )
                }
            },
        )
    }
}

private fun musicSummary(music: MusicConfig): String = when (music.mode) {
    MusicMode.NONE -> "Bez hudby"
    MusicMode.LOCAL -> if (music.tracks.isEmpty()) "Lokálne (0)" else tracksCount(music.tracks.size)
    MusicMode.RADIO -> music.radioName.ifBlank { "Internetové rádio" }
    MusicMode.EXTERNAL -> "Streamovacia appka"
}
