package com.tiborhreus.photoslidesshow.data

enum class TransitionType(override val label: String) : Labeled {
    NONE("Bez prechodu"),
    FADE("Prelínanie"),
    SLIDE_H("Posun vodorovne"),
    SLIDE_V("Posun zvislo"),
    ZOOM_IN("Priblíženie"),
    ZOOM_OUT("Oddialenie"),
    PUSH_FADE("Jemné zasunutie"),
    RANDOM("Náhodný mix");

    companion object {
        /** Prechody, z ktorých sa vyberá v režime "Náhodný mix". */
        val concrete = listOf(FADE, SLIDE_H, SLIDE_V, ZOOM_IN, ZOOM_OUT, PUSH_FADE)
    }
}

enum class SortOrder(override val label: String) : Labeled {
    DATE_ASC("Od najstarších"),
    DATE_DESC("Od najnovších"),
    NAME("Podľa názvu"),
    RANDOM("Náhodne"),
    SOURCE("Podľa zdrojov"),
}

enum class OrientationFilter(override val label: String) : Labeled {
    ALL("Všetky"),
    LANDSCAPE("Len na šírku"),
    PORTRAIT("Len na výšku"),
}

enum class PeriodFilter(override val label: String) : Labeled {
    ALL("Celé obdobie"),
    LAST_WEEK("Posledný týždeň"),
    LAST_MONTH("Posledný mesiac"),
    LAST_YEAR("Posledný rok"),
    ON_THIS_DAY("V tento deň v minulosti"),
}

enum class ScaleMode(override val label: String) : Labeled {
    SMART("Inteligentne"),
    FIT("Celá fotka"),
    FILL("Vyplniť obrazovku"),
}

enum class BackgroundStyle(override val label: String) : Labeled {
    BLUR("Rozmazaná fotka"),
    BLACK("Čierne"),
}

enum class ScreenOrientation(override val label: String) : Labeled {
    AUTO("Podľa systému"),
    SENSOR("Vždy podľa senzora"),
    LANDSCAPE("Na šírku"),
    PORTRAIT("Na výšku"),
}

enum class SleepTimer(val minutes: Int, override val label: String) : Labeled {
    OFF(0, "Vypnutý"),
    M15(15, "15 min"),
    M30(30, "30 min"),
    H1(60, "1 hod"),
    H2(120, "2 hod"),
}

/** Všetky nastavenia spôsobu prezentácie. */
data class SlideshowSettings(
    val displaySeconds: Int = 6,
    val transitionMs: Int = 1000,
    val transition: TransitionType = TransitionType.FADE,
    val kenBurns: Boolean = true,
    val sortOrder: SortOrder = SortOrder.DATE_ASC,
    val orientationFilter: OrientationFilter = OrientationFilter.ALL,
    val periodFilter: PeriodFilter = PeriodFilter.ALL,
    val scaleMode: ScaleMode = ScaleMode.SMART,
    val background: BackgroundStyle = BackgroundStyle.BLUR,
    val pairPortraits: Boolean = true,
    val screenOrientation: ScreenOrientation = ScreenOrientation.AUTO,
    val loop: Boolean = true,
    val showInfo: Boolean = true,
    val showClock: Boolean = false,
    val showProgress: Boolean = true,
    val sleepTimer: SleepTimer = SleepTimer.OFF,
)
