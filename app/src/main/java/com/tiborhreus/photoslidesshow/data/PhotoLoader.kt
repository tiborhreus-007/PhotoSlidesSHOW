package com.tiborhreus.photoslidesshow.data

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Načítanie fotiek zo všetkých typov zdrojov.
 *
 * Orientácia fotiek sa rozpoznáva automaticky:
 *  - MediaStore: stĺpce WIDTH/HEIGHT + ORIENTATION,
 *  - súbory/priečinky (aj z cloudu cez systémový výber): EXIF otočenie + rozmery z hlavičky.
 * Výsledné rozmery sú vždy "tak, ako fotku vidí človek" (po otočení).
 */
object PhotoLoader {

    private val imagesUri: Uri get() = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private const val COL_DATE_TAKEN = "datetaken"
    private const val COL_ORIENTATION = "orientation"

    suspend fun loadAlbums(context: Context): List<AlbumInfo> = withContext(Dispatchers.IO) {
        data class Acc(val name: String, val cover: Uri, var count: Int = 0)

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val map = LinkedHashMap<Long, Acc>()
        context.contentResolver.query(
            imagesUri, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (c.moveToNext()) {
                val bucket = c.getLong(bucketCol)
                val acc = map.getOrPut(bucket) {
                    Acc(
                        name = c.getString(nameCol) ?: "Bez názvu",
                        cover = ContentUris.withAppendedId(imagesUri, c.getLong(idCol)),
                    )
                }
                acc.count++
            }
        }
        map.map { (id, a) -> AlbumInfo(id, a.name, a.count, a.cover) }
            .sortedByDescending { it.count }
    }

    suspend fun loadPhotos(context: Context, sources: List<PhotoSource>): List<PhotoItem> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<PhotoItem>()
            sources.forEachIndexed { order, source ->
                runCatching {
                    when (source) {
                        is PhotoSource.AllPhotos -> queryMediaStore(context, null, null, order)
                        is PhotoSource.Demo -> DemoPhotoGenerator.loadOrCreate(context, order)
                        is PhotoSource.Album -> queryMediaStore(
                            context,
                            "${MediaStore.Images.Media.BUCKET_ID}=?",
                            arrayOf(source.bucketId.toString()),
                            order,
                        )
                        is PhotoSource.Folder -> loadFolder(context, Uri.parse(source.treeUri), order)
                        is PhotoSource.Files -> loadFiles(context, source.uris.map(Uri::parse), order)
                    }
                }.onSuccess { result += it }
            }
            result.distinctBy { it.uri }
        }

    private fun queryMediaStore(
        context: Context,
        selection: String?,
        args: Array<String>?,
        order: Int,
    ): List<PhotoItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            COL_DATE_TAKEN,
            COL_ORIENTATION,
        )
        val out = mutableListOf<PhotoItem>()
        context.contentResolver.query(imagesUri, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val modCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val takenCol = c.getColumnIndex(COL_DATE_TAKEN)
            val orientCol = c.getColumnIndex(COL_ORIENTATION)
            while (c.moveToNext()) {
                val taken = if (takenCol >= 0) c.getLong(takenCol) else 0L
                val modified = c.getLong(modCol) * 1000L
                val rotation = if (orientCol >= 0) c.getInt(orientCol) else 0
                var w = c.getInt(wCol)
                var h = c.getInt(hCol)
                if (rotation == 90 || rotation == 270) { val t = w; w = h; h = t }
                out += PhotoItem(
                    uri = ContentUris.withAppendedId(imagesUri, c.getLong(idCol)),
                    name = c.getString(nameCol) ?: "",
                    dateTaken = if (taken > 0) taken else modified,
                    width = w,
                    height = h,
                    sourceOrder = order,
                )
            }
        }
        return out
    }

    private data class RawDoc(val uri: Uri, val name: String, val modified: Long)

    private suspend fun loadFolder(context: Context, treeUri: Uri, order: Int): List<PhotoItem> {
        val resolver = context.contentResolver
        val docs = mutableListOf<RawDoc>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        fun walk(docId: String, depth: Int) {
            if (depth > 8 || docs.size >= MAX_DOCS) return
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            resolver.query(children, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: ""
                    val mime = c.getString(2) ?: ""
                    val modified = if (c.isNull(3)) 0L else c.getLong(3)
                    when {
                        mime == DocumentsContract.Document.MIME_TYPE_DIR -> walk(id, depth + 1)
                        mime.startsWith("image/") -> docs += RawDoc(
                            DocumentsContract.buildDocumentUriUsingTree(treeUri, id), name, modified
                        )
                    }
                }
            }
        }

        walk(DocumentsContract.getTreeDocumentId(treeUri), 0)
        return enrich(context, docs, order)
    }

    private suspend fun loadFiles(context: Context, uris: List<Uri>, order: Int): List<PhotoItem> {
        val docs = uris.map { uri -> RawDoc(uri, queryDisplayName(context, uri) ?: "", 0L) }
        return enrich(context, docs, order)
    }

    /** Doplní rozmery, otočenie a dátum z EXIF. Spracúva paralelne po dávkach. */
    private suspend fun enrich(context: Context, docs: List<RawDoc>, order: Int): List<PhotoItem> =
        coroutineScope {
            docs.chunked(12).flatMap { chunk ->
                chunk.map { doc -> async(Dispatchers.IO) { readMeta(context, doc, order) } }.awaitAll()
            }
        }

    private fun readMeta(context: Context, doc: RawDoc, order: Int): PhotoItem {
        val resolver = context.contentResolver
        var rotation = 0
        var date = doc.modified
        runCatching {
            resolver.openInputStream(doc.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                rotation = exif.rotationDegrees
                val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                parseExifDate(raw)?.let { date = it }
            }
        }
        var w = 0
        var h = 0
        runCatching {
            resolver.openInputStream(doc.uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, opts)
                w = opts.outWidth
                h = opts.outHeight
            }
        }
        if (rotation == 90 || rotation == 270) { val t = w; w = h; h = t }
        return PhotoItem(doc.uri, doc.name, date, w.coerceAtLeast(0), h.coerceAtLeast(0), order)
    }

    private fun parseExifDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw)?.time
        }.getOrNull()
    }

    fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    /** Názov priečinka vybraného cez systémový výber (SAF). */
    fun queryTreeName(context: Context, treeUri: Uri): String? = runCatching {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri)
        )
        queryDisplayName(context, docUri)
    }.getOrNull()

    private const val MAX_DOCS = 5000
}
