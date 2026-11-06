package com.tiborhreus.photoslidesshow.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.tiborhreus.photoslidesshow.data.AlbumInfo
import com.tiborhreus.photoslidesshow.data.PhotoLoader
import com.tiborhreus.photoslidesshow.data.PhotoSource
import com.tiborhreus.photoslidesshow.data.photosCount
import com.tiborhreus.photoslidesshow.ui.components.AdaptiveColumns
import com.tiborhreus.photoslidesshow.ui.components.BorderedButton
import com.tiborhreus.photoslidesshow.ui.components.BorderedIconButton
import com.tiborhreus.photoslidesshow.ui.components.ScreenScaffold
import com.tiborhreus.photoslidesshow.ui.components.SectionCard
import com.tiborhreus.photoslidesshow.ui.components.toast
import com.tiborhreus.photoslidesshow.ui.theme.AppColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourcesScreen(
    sources: List<PhotoSource>,
    albums: List<AlbumInfo>?,
    onAddSource: (PhotoSource) -> Unit,
    onRemoveSource: (PhotoSource) -> Unit,
    onLoadAlbums: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onLoadAlbums()
        } else {
            context.toast("Prístup k albumom nebol udelený – môžete však vybrať priečinky alebo súbory")
        }
    }

    // Výber priečinka cez systémový SAF (lokálne úložisko, SD karta aj cloud poskytovatelia)
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val folderName = PhotoLoader.queryTreeName(context, uri) ?: "Vybraný priečinok"
            onAddSource(PhotoSource.Folder(uri.toString(), folderName))
        }
    }

    // Výber jednotlivých fotiek (podporuje aj Google Fotky, Google Drive, OneDrive, Dropbox)
    val filesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            val firstTitle = PhotoLoader.queryDisplayName(context, uris.first()) ?: "Vybrané fotky"
            val label = if (uris.size == 1) firstTitle else "Výber fotiek (${uris.size})"
            onAddSource(
                PhotoSource.Files(
                    uris = uris.map { it.toString() },
                    name = label,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            mediaPermission,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm && albums == null) {
            onLoadAlbums()
        }
    }

    ScreenScaffold(title = "Zdroje fotiek", onBack = onBack) {
        AdaptiveColumns(
            left = {
                // Aktívne zdroje
                SectionCard(title = "Aktívne zdroje (${sources.size})", icon = Icons.Filled.Collections) {
                    if (sources.isEmpty()) {
                        Text(
                            "Zatiaľ nie je pridaný žiadny zdroj. Pridajte priečinok, album, cloudové fotky alebo ukážkovú kolekciu.",
                            color = AppColors.TextMuted,
                        )
                    } else {
                        sources.forEach { src ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AppColors.SurfaceHigh,
                                border = BorderStroke(1.5.dp, AppColors.Border.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            src.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            src.kind,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.TextMuted,
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    BorderedIconButton(
                                        icon = Icons.Filled.DeleteOutline,
                                        contentDescription = "Odstrániť zdroj",
                                        danger = true,
                                        size = 42.dp,
                                        onClick = { onRemoveSource(src) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Pridanie lokálneho alebo cloudového zdroja
                SectionCard(title = "Pridať fotky (Lokálne & Cloud)", icon = Icons.Filled.CloudQueue) {
                    Text(
                        "Systémový výber Androidu (SAF / Photo Picker) priamo sprístupňuje nielen interné úložisko a SD kartu, ale aj nainštalované cloudové služby: Google Fotky, Google Drive, OneDrive, Dropbox či Nextcloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )

                    BorderedButton(
                        text = "Vybrať priečinok (Lokálne / Cloud)",
                        subtitle = "Načíta fotky vrátane podpriečinkov",
                        icon = Icons.Filled.CreateNewFolder,
                        primary = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { folderPicker.launch(null) },
                    )

                    BorderedButton(
                        text = "Vybrať fotky (Zariadenie / Google Fotky / Cloud)",
                        subtitle = "Výber konkrétnych fotiek z galérie alebo cloudu",
                        icon = Icons.Filled.Image,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { filesPicker.launch(arrayOf("image/*")) },
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val hasAll = sources.any { it is PhotoSource.AllPhotos }
                        BorderedButton(
                            text = if (hasAll) "Všetky fotky pridané" else "Všetky fotky v zariadení",
                            icon = Icons.Filled.PhotoLibrary,
                            enabled = !hasAll,
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    mediaPermission,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    permissionLauncher.launch(mediaPermission)
                                }
                                onAddSource(PhotoSource.AllPhotos)
                            },
                        )

                        val hasDemo = sources.any { it is PhotoSource.Demo }
                        BorderedButton(
                            text = if (hasDemo) "Ukážková kolekcia aktívna" else "Pridať ukážkovú kolekciu (Demo)",
                            icon = Icons.Filled.Add,
                            enabled = !hasDemo,
                            onClick = { onAddSource(PhotoSource.Demo) },
                        )
                    }
                }
            },
            right = {
                // Albumy v zariadení
                SectionCard(title = "Albumy v zariadení", icon = Icons.Filled.PhotoLibrary) {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        mediaPermission,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPerm) {
                        Text(
                            "Pre zobrazenie zoznamu albumov v telefóne/tablete povoľte prístup k fotkám.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextMuted,
                        )
                        BorderedButton(
                            text = "Povoliť prístup k albumom",
                            icon = Icons.Filled.PhotoLibrary,
                            primary = true,
                            onClick = { permissionLauncher.launch(mediaPermission) },
                        )
                    } else if (albums == null) {
                        Text("Načítavam albumy…", color = AppColors.TextMuted)
                    } else if (albums.isEmpty()) {
                        Text("V zariadení sa nenašli žiadne albumy.", color = AppColors.TextMuted)
                    } else {
                        albums.forEach { album ->
                            val added = sources.any {
                                it is PhotoSource.Album && it.bucketId == album.bucketId
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AppColors.SurfaceHigh,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (added) AppColors.Accent else AppColors.BorderDim,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = album.coverUri,
                                        contentDescription = album.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            album.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            photosCount(album.count),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.TextMuted,
                                        )
                                    }
                                    BorderedButton(
                                        text = if (added) "Pridané" else "Pridať",
                                        enabled = !added,
                                        primary = !added,
                                        onClick = {
                                            onAddSource(
                                                PhotoSource.Album(
                                                    bucketId = album.bucketId,
                                                    name = album.name,
                                                    count = album.count,
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}
