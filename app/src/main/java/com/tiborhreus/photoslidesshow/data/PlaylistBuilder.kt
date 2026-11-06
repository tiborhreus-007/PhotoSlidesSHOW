package com.tiborhreus.photoslidesshow.data

import java.util.Calendar

/** Filtrovanie, triedenie a skladanie snímok podľa nastavení. */
object PlaylistBuilder {

    private const val DAY_MS = 86_400_000L

    fun build(
        all: List<PhotoItem>,
        s: SlideshowSettings,
        now: Long = System.currentTimeMillis(),
    ): List<PhotoItem> {
        val byOrientation = all.filter { p ->
            when (s.orientationFilter) {
                OrientationFilter.ALL -> true
                // Fotky s neznámymi rozmermi radšej nevyraďujeme.
                OrientationFilter.LANDSCAPE -> !p.hasSize || p.isLandscape
                OrientationFilter.PORTRAIT -> !p.hasSize || p.isPortrait
            }
        }
        val byPeriod = filterPeriod(byOrientation, s.periodFilter, now)
        return sort(byPeriod, s.sortOrder)
    }

    fun sort(list: List<PhotoItem>, order: SortOrder): List<PhotoItem> = when (order) {
        SortOrder.DATE_ASC -> list.sortedBy { it.dateTaken }
        SortOrder.DATE_DESC -> list.sortedByDescending { it.dateTaken }
        SortOrder.NAME -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOrder.RANDOM -> list.shuffled()
        SortOrder.SOURCE -> list.sortedWith(compareBy<PhotoItem>({ it.sourceOrder }, { it.dateTaken }))
    }

    private fun filterPeriod(list: List<PhotoItem>, period: PeriodFilter, now: Long): List<PhotoItem> =
        when (period) {
            PeriodFilter.ALL -> list
            PeriodFilter.LAST_WEEK -> list.filter { it.dateTaken >= now - 7 * DAY_MS }
            PeriodFilter.LAST_MONTH -> list.filter { it.dateTaken >= now - 30 * DAY_MS }
            PeriodFilter.LAST_YEAR -> list.filter { it.dateTaken >= now - 365 * DAY_MS }
            PeriodFilter.ON_THIS_DAY -> {
                val today = Calendar.getInstance().apply { timeInMillis = now }
                val cal = Calendar.getInstance()
                list.filter { p ->
                    if (p.dateTaken <= 0) return@filter false
                    cal.timeInMillis = p.dateTaken
                    cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) &&
                        cal.get(Calendar.YEAR) < today.get(Calendar.YEAR)
                }
            }
        }

    /**
     * Zostaví snímky. Ak je zapnuté spájanie, dve fotky na výšku sa zobrazia vedľa seba
     * (používa sa len keď je obrazovka na šírku).
     */
    fun buildSlides(photos: List<PhotoItem>, pairPortraits: Boolean): List<Slide> {
        if (!pairPortraits) return photos.map { Slide(listOf(it)) }
        val out = mutableListOf<Slide>()
        var pending: PhotoItem? = null
        for (p in photos) {
            if (p.isPortrait) {
                val first = pending
                if (first == null) {
                    pending = p
                } else {
                    out += Slide(listOf(first, p))
                    pending = null
                }
            } else {
                out += Slide(listOf(p))
            }
        }
        pending?.let { out += Slide(listOf(it)) }
        return out
    }
}
