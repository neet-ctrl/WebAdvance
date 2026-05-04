package com.cylonid.nativealpha.viewmodel

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.DownloadItemDao
import com.cylonid.nativealpha.manager.DownloadItem
import com.cylonid.nativealpha.manager.DownloadManager
import com.cylonid.nativealpha.manager.FileViewerManager
import com.cylonid.nativealpha.repository.WebAppRepository
import com.cylonid.nativealpha.util.StorageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadItemDao,
    private val downloadManager: DownloadManager,
    private val fileViewerManager: FileViewerManager,
    private val webAppRepository: WebAppRepository
) : ViewModel() {

    enum class SortBy {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC
    }

    enum class FilterBy {
        ALL, COMPLETED, DOWNLOADING, FAILED, IMAGES, VIDEOS, DOCUMENTS, FOLDERS, SCREENSHOTS
    }

    data class FileSystemItem(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long = 0,
        val lastModified: Long = System.currentTimeMillis(),
        val mimeType: String? = null,
        val icon: String = if (isDirectory) "📁" else "📄"
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortBy = MutableStateFlow(SortBy.DATE_DESC)
    val sortBy: StateFlow<SortBy> = _sortBy

    private val _filterBy = MutableStateFlow(FilterBy.ALL)
    val filterBy: StateFlow<FilterBy> = _filterBy

    private val _fileSystemItems = MutableStateFlow<List<FileSystemItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentFolderPath = MutableStateFlow<String?>(null)
    val currentFolderPath: StateFlow<String?> = _currentFolderPath

    private val _rootFolderPath = MutableStateFlow<String?>(null)
    val rootFolderPath: StateFlow<String?> = _rootFolderPath
    
    private var currentAppId: Long? = null
    private var currentAppName: String? = null

    val downloads: StateFlow<List<FileSystemItem>> = combine(
        _fileSystemItems,
        _searchQuery,
        _sortBy,
        _filterBy
    ) { items, query, sort, filter ->
        var filtered = items

        // Apply search filter
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Apply filter
        filtered = filtered.filter { item ->
            when (filter) {
                FilterBy.ALL -> true
                FilterBy.FOLDERS -> item.isDirectory
                FilterBy.SCREENSHOTS -> item.path.contains("Screenshots", ignoreCase = true)
                FilterBy.IMAGES -> !item.isDirectory && item.mimeType?.startsWith("image/") == true
                FilterBy.VIDEOS -> !item.isDirectory && item.mimeType?.startsWith("video/") == true
                FilterBy.DOCUMENTS -> !item.isDirectory && (
                    item.mimeType?.startsWith("text/") == true ||
                    item.mimeType == "application/pdf" ||
                    item.mimeType?.contains("document") == true
                )
                FilterBy.COMPLETED, FilterBy.DOWNLOADING, FilterBy.FAILED -> true // Legacy support
            }
        }

        // Apply sorting
        filtered.sortedWith { a, b ->
            when (sort) {
                SortBy.DATE_DESC -> b.lastModified.compareTo(a.lastModified)
                SortBy.DATE_ASC -> a.lastModified.compareTo(b.lastModified)
                SortBy.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                SortBy.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                SortBy.SIZE_DESC -> b.size.compareTo(a.size)
                SortBy.SIZE_ASC -> a.size.compareTo(b.size)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadDownloads(webAppId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            currentAppId = webAppId
            val webApp = webAppRepository.getWebAppById(webAppId).firstOrNull()
            currentAppName = webApp?.name ?: "Unknown"

            currentAppName?.let { appName ->
                val rootDir = StorageUtil.getAppDownloadsDir(context, appName)
                _rootFolderPath.value = rootDir.absolutePath
                _currentFolderPath.value = rootDir.absolutePath
            }

            // Scan the file system folder
            scanAppDownloadsFolder(_currentFolderPath.value)
            _isLoading.value = false
        }
    }

    fun navigateToFolder(folderPath: String) {
        viewModelScope.launch {
            _currentFolderPath.value = folderPath
            scanAppDownloadsFolder(folderPath)
        }
    }

    fun navigateUp() {
        val currentPath = _currentFolderPath.value
        val rootPath = _rootFolderPath.value
        if (currentPath == null || rootPath == null || currentPath == rootPath) return

        val parentFile = File(currentPath).parentFile
        if (parentFile != null && parentFile.exists() && parentFile.absolutePath.startsWith(rootPath)) {
            _currentFolderPath.value = parentFile.absolutePath
            scanAppDownloadsFolder(parentFile.absolutePath)
        }
    }

    private fun scanAppDownloadsFolder(folderPath: String?) {
        currentAppName?.let { _ ->
            val storageItems = mutableListOf<FileSystemItem>()
            if (folderPath != null) {
                val folder = File(folderPath)
                if (folder.exists() && folder.isDirectory) {
                    folder.listFiles()?.forEach { file ->
                        val mimeType = if (file.isDirectory) null else getMimeType(file.name)
                        storageItems.add(
                            FileSystemItem(
                                name = file.name,
                                path = file.absolutePath,
                                isDirectory = file.isDirectory,
                                size = if (file.isDirectory) calculateDirSize(file) else file.length(),
                                lastModified = file.lastModified(),
                                mimeType = mimeType,
                                icon = getIconForMimeType(mimeType, file.isDirectory)
                            )
                        )
                    }
                }
            }

            _fileSystemItems.value = storageItems
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirSize(file) else file.length()
            }
        }
        return size
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp|bmp)$", RegexOption.IGNORE_CASE)) -> "image/*"
            fileName.matches(Regex(".*\\.(mp4|mkv|webm|avi|mov|flv|wmv)$", RegexOption.IGNORE_CASE)) -> "video/*"
            fileName.matches(Regex(".*\\.(mp3|wav|aac|flac|opus|m4a|ogg)$", RegexOption.IGNORE_CASE)) -> "audio/*"
            fileName.matches(Regex(".*\\.pdf$", RegexOption.IGNORE_CASE)) -> "application/pdf"
            fileName.matches(Regex(".*\\.(txt|log|md|csv|json|xml|html)$", RegexOption.IGNORE_CASE)) -> "text/*"
            fileName.matches(Regex(".*\\.(zip|rar|7z|tar|gz)$", RegexOption.IGNORE_CASE)) -> "application/archive"
            fileName.matches(Regex(".*\\.apk$", RegexOption.IGNORE_CASE)) -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    private fun getIconForMimeType(mimeType: String?, isDirectory: Boolean): String {
        return when {
            isDirectory -> "📁"
            mimeType?.startsWith("image/") == true -> "🖼️"
            mimeType?.startsWith("video/") == true -> "🎬"
            mimeType?.startsWith("audio/") == true -> "🎵"
            mimeType == "application/pdf" -> "📕"
            mimeType?.startsWith("text/") == true -> "📄"
            mimeType?.contains("archive") == true -> "📦"
            mimeType?.contains("android.package") == true -> "🔧"
            else -> "📄"
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
    }

    fun updateFilterBy(filterBy: FilterBy) {
        _filterBy.value = filterBy
    }

    fun openFile(item: FileSystemItem) {
        viewModelScope.launch {
            if (!item.isDirectory) {
                fileViewerManager.openFile(File(item.path))
            }
        }
    }

    fun openFolder(item: FileSystemItem) {
        if (!item.isDirectory) return
        navigateToFolder(item.path)
    }

    fun reloadFiles() {
        scanAppDownloadsFolder(_currentFolderPath.value)
    }

    fun shareFile(item: FileSystemItem) {
        viewModelScope.launch {
            if (!item.isDirectory) {
                fileViewerManager.shareFile(File(item.path))
            }
        }
    }

    fun deleteFile(item: FileSystemItem) {
        viewModelScope.launch {
            val file = File(item.path)
            if (file.exists()) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                scanAppDownloadsFolder(_currentFolderPath.value)
            }
        }
    }

    fun duplicateFile(item: FileSystemItem) {
        viewModelScope.launch {
            val file = File(item.path)
            if (file.exists() && !file.isDirectory) {
                val copyName = file.nameWithoutExtension + " (copy)" + if (file.extension.isNotBlank()) ".${file.extension}" else ""
                val duplicateFile = File(file.parentFile, copyName)
                if (!duplicateFile.exists()) {
                    file.copyTo(duplicateFile)
                    scanAppDownloadsFolder(_currentFolderPath.value)
                }
            }
        }
    }

    fun renameFile(item: FileSystemItem, newName: String) {
        viewModelScope.launch {
            val file = File(item.path)
            val newFile = File(file.parent, newName)
            if (file.exists() && !newFile.exists()) {
                file.renameTo(newFile)
                scanAppDownloadsFolder(_currentFolderPath.value)
            }
        }
    }

    fun getFileSize(item: FileSystemItem): String = StorageUtil.formatFileSize(item.size)

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    fun exportFileTo(item: FileSystemItem, treeUri: Uri) {
        viewModelScope.launch {
            try {
                val sourceFile = File(item.path)
                if (!sourceFile.exists() || sourceFile.isDirectory) {
                    _exportMessage.value = "Cannot export folder directly — use Export All inside the folder"
                    return@launch
                }
                val tree = DocumentFile.fromTreeUri(context, treeUri)
                if (tree == null || !tree.canWrite()) {
                    _exportMessage.value = "Cannot write to selected folder"
                    return@launch
                }
                val mimeType = item.mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
                val newDoc = tree.createFile(mimeType, item.name)
                if (newDoc == null) {
                    _exportMessage.value = "Failed to create file in destination"
                    return@launch
                }
                context.contentResolver.openOutputStream(newDoc.uri)?.use { out ->
                    sourceFile.inputStream().use { it.copyTo(out) }
                }
                _exportMessage.value = "✓ Exported: ${item.name}"
            } catch (e: Exception) {
                _exportMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun exportFolderTo(folderPath: String, treeUri: Uri) {
        viewModelScope.launch {
            try {
                val sourceFolder = File(folderPath)
                if (!sourceFolder.exists() || !sourceFolder.isDirectory) {
                    _exportMessage.value = "Folder not found"
                    return@launch
                }
                val tree = DocumentFile.fromTreeUri(context, treeUri)
                if (tree == null || !tree.canWrite()) {
                    _exportMessage.value = "Cannot write to selected folder"
                    return@launch
                }
                val count = copyFolderContentsTo(sourceFolder, tree)
                _exportMessage.value = if (count > 0) "✓ Exported $count file(s) to selected folder"
                                       else "No files to export in this folder"
            } catch (e: Exception) {
                _exportMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    private fun copyFolderContentsTo(sourceFolder: File, destTree: DocumentFile): Int {
        var count = 0
        sourceFolder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val subTree = destTree.createDirectory(file.name) ?: return@forEach
                count += copyFolderContentsTo(file, subTree)
            } else {
                val mimeType = getMimeType(file.name).let {
                    if (it == "image/*") "image/jpeg"
                    else if (it.endsWith("/*")) "application/octet-stream"
                    else it
                }
                val newDoc = destTree.createFile(mimeType, file.name) ?: return@forEach
                try {
                    context.contentResolver.openOutputStream(newDoc.uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    }
                    count++
                } catch (_: Exception) {}
            }
        }
        return count
    }
}