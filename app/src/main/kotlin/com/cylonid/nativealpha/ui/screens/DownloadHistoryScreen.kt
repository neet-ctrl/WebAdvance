package com.cylonid.nativealpha.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.manager.DownloadItem
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.util.StorageUtil
import com.cylonid.nativealpha.viewmodel.DownloadViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    webAppId: Long,
    webAppDisplayName: String = "App",
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val fileItems by viewModel.downloads.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val filterBy by viewModel.filterBy.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentFolderPath by viewModel.currentFolderPath.collectAsState()
    val rootFolderPath by viewModel.rootFolderPath.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    var pendingExportItem by remember { mutableStateOf<DownloadViewModel.FileSystemItem?>(null) }

    val exportFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val item = pendingExportItem
        if (uri != null && item != null) {
            viewModel.exportFileTo(item, uri)
        }
        pendingExportItem = null
    }

    LaunchedEffect(exportMessage) {
        if (exportMessage != null) {
            android.widget.Toast.makeText(context, exportMessage, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearExportMessage()
        }
    }

    LaunchedEffect(webAppId) {
        viewModel.loadDownloads(webAppId)
    }

    val filterOptions = listOf(
        DownloadViewModel.FilterBy.ALL to "All",
        DownloadViewModel.FilterBy.FOLDERS to "Folders",
        DownloadViewModel.FilterBy.SCREENSHOTS to "Screenshots",
        DownloadViewModel.FilterBy.IMAGES to "Images",
        DownloadViewModel.FilterBy.VIDEOS to "Videos",
        DownloadViewModel.FilterBy.DOCUMENTS to "Docs"
    )

    val totalFileCount = fileItems.size
    val totalSize = fileItems.sumOf { it.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(BgDark, BgDeep))
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.radialGradient(listOf(GradCyanStart.copy(0.4f), Color.Transparent)),
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, GradCyanEnd.copy(0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Download, null, tint = GradCyanEnd, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("$webAppDisplayName Files", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("$totalFileCount items · ${StorageUtil.formatFileSize(totalSize)}", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        if (currentFolderPath != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentFolderPath != rootFolderPath) {
                                    IconButton(
                                        onClick = { viewModel.navigateUp() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanPrimary)
                                    }
                                }
                                Text(
                                    text = if (currentFolderPath == rootFolderPath) {
                                        "Current folder: $webAppDisplayName"
                                    } else {
                                        File(currentFolderPath ?: "").name.ifBlank { currentFolderPath ?: "Downloads" }
                                    },
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search downloads…", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CardBorder,
                        cursorColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sort:", color = TextMuted, fontSize = 12.sp)
                    var sortExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = sortExpanded,
                        onExpandedChange = { sortExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (sortBy) {
                                DownloadViewModel.SortBy.DATE_DESC -> "Newest"
                                DownloadViewModel.SortBy.DATE_ASC -> "Oldest"
                                DownloadViewModel.SortBy.NAME_ASC -> "A–Z"
                                DownloadViewModel.SortBy.NAME_DESC -> "Z–A"
                                DownloadViewModel.SortBy.SIZE_DESC -> "Largest"
                                DownloadViewModel.SortBy.SIZE_ASC -> "Smallest"
                            },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .weight(1f),
                            trailingIcon = {
                                Icon(
                                    if (sortExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                        ExposedDropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            listOf(
                                DownloadViewModel.SortBy.DATE_DESC to "Newest first",
                                DownloadViewModel.SortBy.DATE_ASC to "Oldest first",
                                DownloadViewModel.SortBy.NAME_ASC to "Name A–Z",
                                DownloadViewModel.SortBy.NAME_DESC to "Name Z–A",
                                DownloadViewModel.SortBy.SIZE_DESC to "Largest first",
                                DownloadViewModel.SortBy.SIZE_ASC to "Smallest first"
                            ).forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = if (sortBy == value) CyanPrimary else TextPrimary, fontSize = 13.sp) },
                                    onClick = { viewModel.updateSortBy(value); sortExpanded = false },
                                    leadingIcon = if (sortBy == value) ({
                                        Icon(Icons.Default.Check, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                                    }) else null
                                )
                            }
                        }
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgDark)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { (value, label) ->
                val isSelected = filterBy == value
                val bgColor by animateColorAsState(
                    if (isSelected) CyanPrimary.copy(0.2f) else CardSurface,
                    tween(200)
                )
                val borderColor by animateColorAsState(
                    if (isSelected) CyanPrimary else CardBorder,
                    tween(200)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        color = if (isSelected) CyanPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    androidx.compose.material3.Surface(
                        modifier = Modifier.matchParentSize(),
                        color = Color.Transparent,
                        onClick = { viewModel.updateFilterBy(value) }
                    ) {}
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (fileItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(CardSurface, RoundedCornerShape(20.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Download, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No files yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Files will appear here when downloaded", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(fileItems, key = { it.path }) { item ->
                    FileSystemItemCard(
                        item = item,
                        onOpen = {
                            if (item.isDirectory) viewModel.openFolder(item) else viewModel.openFile(item)
                        },
                        onShare = { viewModel.shareFile(item) },
                        onDuplicate = { viewModel.duplicateFile(item) },
                        onDelete = { viewModel.deleteFile(item) },
                        onExport = if (item.isDirectory) null else ({
                            pendingExportItem = item
                            exportFolderLauncher.launch(null)
                        })
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    download: DownloadItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val (iconRes, gradStart, gradEnd) = getDownloadTypeStyle(download.mimeType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.radialGradient(listOf(gradStart.copy(0.35f), Color.Transparent)),
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, gradStart.copy(0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconRes, null, tint = gradEnd, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        download.fileName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatTimestamp(download.timestamp),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                DownloadStatusBadge(download.status)
            }

            Spacer(Modifier.height(10.dp))

            when (download.status) {
                DownloadItem.Status.DOWNLOADING -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${download.progressPercentage}%  ·  ${download.formattedDownloadedSize} / ${download.formattedFileSize}",
                                color = CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (download.formattedSpeed.isNotEmpty()) {
                                Text(
                                    "${download.formattedSpeed}  ·  ETA ${download.formattedETA}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { download.progressPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = CyanPrimary,
                            trackColor = CyanPrimary.copy(0.15f)
                        )
                    }
                }
                DownloadItem.Status.COMPLETED -> {
                    Text(download.formattedFileSize, color = TextSecondary, fontSize = 12.sp)
                }
                else -> {
                    Text(download.formattedFileSize, color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider(color = CardBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.status) {
                    DownloadItem.Status.COMPLETED -> {
                        WaosSmallBtn("Open", Icons.Default.OpenInNew, CyanPrimary, onOpen)
                        Spacer(Modifier.width(6.dp))
                        WaosSmallBtn("Share", Icons.Default.Share, VioletSecondary, onShare)
                    }
                    DownloadItem.Status.DOWNLOADING -> {
                        WaosSmallBtn("Pause", Icons.Default.Pause, Color(0xFFFFB800), onPause)
                        Spacer(Modifier.width(6.dp))
                        WaosSmallBtn("Cancel", Icons.Default.Close, ErrorRed, onCancel)
                    }
                    DownloadItem.Status.PAUSED -> {
                        WaosSmallBtn("Resume", Icons.Default.PlayArrow, StatusActive, onResume)
                        Spacer(Modifier.width(6.dp))
                        WaosSmallBtn("Cancel", Icons.Default.Close, ErrorRed, onCancel)
                    }
                    DownloadItem.Status.FAILED -> {
                        WaosSmallBtn("Retry", Icons.Default.Refresh, Color(0xFFFFB800), onRetry)
                    }
                    else -> {}
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = ErrorRed.copy(0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun FileSystemItemCard(
    item: com.cylonid.nativealpha.viewmodel.DownloadViewModel.FileSystemItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.radialGradient(listOf(GradCyanStart.copy(0.35f), Color.Transparent)),
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, GradCyanStart.copy(0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.icon, fontSize = 24.sp)
                }

                // Name and size
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (item.isDirectory) "Folder" else StorageUtil.formatFileSize(item.size),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            "•",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            formatTimestamp(item.lastModified),
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onExport != null) {
                        IconButton(
                            onClick = onExport,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.SaveAlt,
                                contentDescription = "Export to folder",
                                tint = GradGreenStart,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.FileCopy,
                            contentDescription = "Duplicate",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaosSmallBtn(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            onClick = onClick
        ) {}
    }
}

@Composable
private fun DownloadStatusBadge(status: DownloadItem.Status) {
    val (label, color) = when (status) {
        DownloadItem.Status.COMPLETED -> "Done" to StatusActive
        DownloadItem.Status.DOWNLOADING -> "Active" to CyanPrimary
        DownloadItem.Status.PAUSED -> "Paused" to Color(0xFFFFB800)
        DownloadItem.Status.FAILED -> "Failed" to ErrorRed
        DownloadItem.Status.PENDING -> "Queued" to TextMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(0.15f))
            .border(1.dp, color.copy(0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private data class DownloadTypeStyle(val icon: ImageVector, val gradStart: Color, val gradEnd: Color)

private fun getDownloadTypeStyle(mimeType: String?): DownloadTypeStyle {
    return when {
        mimeType?.startsWith("image/") == true -> DownloadTypeStyle(Icons.Default.Image, GradPinkStart, GradPinkEnd)
        mimeType?.startsWith("video/") == true -> DownloadTypeStyle(Icons.Default.Movie, GradVioletStart, GradVioletEnd)
        mimeType?.startsWith("audio/") == true -> DownloadTypeStyle(Icons.Default.MusicNote, GradCyanStart, GradCyanEnd)
        mimeType == "application/pdf" -> DownloadTypeStyle(Icons.Default.PictureAsPdf, GradRedStart, GradRedEnd)
        mimeType?.startsWith("text/") == true -> DownloadTypeStyle(Icons.Default.Description, GradBlueStart, GradBlueEnd)
        mimeType == "application/vnd.android.package-archive" -> DownloadTypeStyle(Icons.Default.Android, GradGreenStart, GradGreenEnd)
        else -> DownloadTypeStyle(Icons.Default.InsertDriveFile, GradOrangeStart, GradOrangeEnd)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}
