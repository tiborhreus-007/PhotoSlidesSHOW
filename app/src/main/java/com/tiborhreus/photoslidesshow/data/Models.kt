package com.tiborhreus.photoslidesshow.data

import android.net.Uri

/** Spoločné rozhranie pre voľby zobrazované ako "chips". */
interface Labeled {
    val label: String
}

/** Jedna fotka pripravená na prezentáciu. Rozmery sú už prepočítané podľa EXIF otočenia. */
data class PhotoItem(
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val width: Int,
    val height: Int,
    val sourceOrder: Int,
) {
    val hasSize: Boolean get() = width > 0 && height > 0
    val isPortrait: Boolean get() = hasSize && height > width
    val isLandscape: Boolean get() = hasSize && width >= height
    val aspect: Float get() = if (hasSize) width.toFloat() / height else 0f
}

/** Snímka prezentácie – jedna fotka, alebo dvojica fotiek na výšku vedľa seba. */
data class Slide(val photos: List<PhotoItem>) {
    val key: String = photos.joinToString("|") { it.uri.toString() }
}

data class AlbumInfo(
    val bucketId: Long,
    val name: String,
    val count: Int,
    val coverUri: Uri,
)

/** Zdroj fotiek, ktorý si používateľ pridal. */
sealed class PhotoSource {
    abstract val key: String
    abstract val label: String
    abstract val kind: String

    data object AllPhotos : PhotoSource() {
        override val key = "all"
        override val label = "Všetky fotky v zariadení"
        override val kind = "Zariadenie"
    }

    data class Album(val bucketId: Long, val name: String, val count: Int) : PhotoSource() {
        override val key get() = "album:$bucketId"
        override val label get() = name
        override val kind get() = "Album • ${photosCount(count)}"
    }

    data class Folder(val treeUri: String, val name: String) : PhotoSource() {
        override val key get() = "folder:$treeUri"
        override val label get() = name
        override val kind get() = "Priečinok (vrátane podpriečinkov)"
    }

    data class Files(val uris: List<String>, val name: String, val createdAt: Long) : PhotoSource() {
        override val key get() = "files:$createdAt"
        override val label get() = name
        override val kind get() = "Vybrané súbory • ${photosCount(uris.size)}"
    }

    data object Demo : PhotoSource() {
        override val key = "demo"
        override val label = "Ukážková kolekcia (Na šírku + Na výšku)"
        override val kind = "Vstavané demo • 8 fotiek"
    }
}

/** Stav načítania zoznamu fotiek pre prezentáciu. */
sealed interface PlaylistState {
    data object Idle : PlaylistState
    data object Loading : PlaylistState
    data class Ready(val photos: List<PhotoItem>, val generation: Long) : PlaylistState
    data class Empty(val totalBeforeFilter: Int) : PlaylistState
    data class Error(val message: String) : PlaylistState
}

/** Slovenské skloňovanie počtu fotiek. */
fun photosCount(n: Int): String = when {
    n == 1 -> "1 fotka"
    n in 2..4 -> "$n fotky"
    else -> "$n fotiek"
}

fun tracksCount(n: Int): String = when {
    n == 1 -> "1 skladba"
    n in 2..4 -> "$n skladby"
    else -> "$n skladieb"
}
