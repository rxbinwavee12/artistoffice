package com.hb.office

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class StorageItem(
    val id: String = "",
    val name: String = "",
    val isFolder: Boolean = false,
    val downloadUrl: String = "",
    val parentFolderId: String = "",
    val category: String = "Alle",
    val status: String = "",
    val lyrics: String = "",
    val timestamp: Long = 0
)

data class FileComment(
    val id: String = "",
    val author: String = "",
    val text: String = "",
    val timestamp: Long = 0
)

class StorageViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _items = MutableStateFlow<List<StorageItem>>(emptyList())
    val items: StateFlow<List<StorageItem>> = _items.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private var currentListener: ListenerRegistration? = null

    fun loadItems(projectName: String, parentFolderId: String = "") {
        currentListener?.remove()

        val itemsRef = db.collection("projects").document(projectName).collection("items")

        currentListener = itemsRef
            .whereEqualTo("parentFolderId", parentFolderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _items.value = emptyList()
                    return@addSnapshotListener
                }

                // Falls ein Projekt/Ordner in der App noch jungfräulich leer ist,
                // legen wir beim ersten Öffnen automatisch die Grundstruktur an!
                if (snapshot.isEmpty && parentFolderId.isBlank()) {
                    if (projectName.equals("Hindenburg", ignoreCase = true)) {
                        createFolder(projectName, "Demos & Ideas", "", "Demo")
                        createFolder(projectName, "Stems & Einzelspuren", "", "Recording")
                        createFolder(projectName, "Mixes & Pre-Masters", "", "Mixing")
                        createFolder(projectName, "Final Masters", "", "Mastered")
                    } else if (projectName.equals("SchuppenRecords", ignoreCase = true)) {
                        createFolder(projectName, "Releases 2025", "", "Released")
                        createFolder(projectName, "Promo & Artwork", "", "Mastered")
                    }
                }

                val itemList = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: "Unbekannt"
                    val isFolder = doc.getBoolean("isFolder") ?: false
                    val url = doc.getString("downloadUrl") ?: ""
                    val parentId = doc.getString("parentFolderId") ?: ""
                    val category = doc.getString("category") ?: "Sonstiges"
                    val status = doc.getString("status") ?: ""
                    val lyrics = doc.getString("lyrics") ?: ""
                    val time = doc.getLong("timestamp") ?: 0L

                    StorageItem(
                        id = doc.id,
                        name = name,
                        isFolder = isFolder,
                        downloadUrl = url,
                        parentFolderId = parentId,
                        category = category,
                        status = status,
                        lyrics = lyrics,
                        timestamp = time
                    )
                }.sortedWith(compareByDescending<StorageItem> { it.isFolder }.thenBy { it.name.lowercase() })

                _items.value = itemList
            }
    }

    fun updateLyrics(projectName: String, itemId: String, lyrics: String) {
        db.collection("projects").document(projectName)
            .collection("items").document(itemId)
            .update("lyrics", lyrics.trim())
    }

    fun createFolder(projectName: String, folderName: String, parentFolderId: String, status: String = "Demo") {
        if (folderName.isBlank()) return

        val docRef = db.collection("projects").document(projectName).collection("items").document()
        val folderData = mapOf(
            "name" to folderName.trim(),
            "isFolder" to true,
            "downloadUrl" to "",
            "parentFolderId" to parentFolderId,
            "status" to status,
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(folderData)
    }

    fun addDriveLink(projectName: String, name: String, driveUrl: String, parentFolderId: String, category: String = "Sonstiges") {
        if (name.isBlank() || driveUrl.isBlank()) return
        val docRef = db.collection("projects").document(projectName).collection("items").document()

        val fileData = mapOf(
            "name" to name.trim(),
            "isFolder" to false,
            "downloadUrl" to driveUrl.trim(),
            "parentFolderId" to parentFolderId,
            "category" to category,
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(fileData)
    }

    fun uploadLocalFileToCloud(
        context: Context,
        projectName: String,
        parentFolderId: String,
        fileUri: Uri,
        fileName: String,
        category: String = "Sonstiges"
    ) {
        _isUploading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    val boundary = "*****" + System.currentTimeMillis() + "*****"
                    val url = URL("https://catbox.moe/user/api.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.useCaches = false
                    conn.setRequestProperty("Connection", "Keep-Alive")
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")

                    val os = conn.outputStream
                    val writer = java.io.PrintWriter(java.io.OutputStreamWriter(os, "UTF-8"), true)

                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"reqtype\"\r\n\r\n")
                    writer.append("fileupload\r\n")

                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"$fileName\"\r\n")
                    writer.append("Content-Type: application/octet-stream\r\n\r\n")
                    writer.flush()

                    os.write(bytes)
                    os.flush()

                    writer.append("\r\n")
                    writer.append("--$boundary--\r\n")
                    writer.flush()
                    writer.close()

                    if (conn.responseCode == 200) {
                        val cloudUrl = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                        conn.disconnect()

                        if (cloudUrl.startsWith("http")) {
                            addDriveLink(projectName, fileName, cloudUrl, parentFolderId, category)
                        }
                    } else {
                        conn.disconnect()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    _isUploading.value = false
                }
            }
        }
    }

    fun deleteItem(projectName: String, item: StorageItem) {
        val itemsRef = db.collection("projects").document(projectName).collection("items")

        // 1. In Firestore löschen
        itemsRef.document(item.id).delete()

        // 2. Falls es ein Ordner ist, rekursiv alle Unter-Items löschen; falls Datei, auf Catbox löschen
        if (item.isFolder) {
            deleteFolderRecursive(projectName, item.id)
        } else if (item.downloadUrl.contains("catbox.moe")) {
            val fileNameOnServer = item.downloadUrl.substringAfterLast("/")
            if (fileNameOnServer.isNotBlank()) {
                deleteFileFromCatbox(fileNameOnServer)
            }
        }
    }

    private fun deleteFileFromCatbox(fileNameOnServer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val boundary = "*****" + System.currentTimeMillis() + "*****"
                val url = URL("https://catbox.moe/user/api.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.useCaches = false
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")

                val os = conn.outputStream
                val writer = java.io.PrintWriter(java.io.OutputStreamWriter(os, "UTF-8"), true)

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"reqtype\"\r\n\r\n")
                writer.append("deletefiles\r\n")

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"files\"\r\n\r\n")
                writer.append("$fileNameOnServer\r\n")

                writer.append("--$boundary--\r\n")
                writer.flush()
                writer.close()

                conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun deleteFolderRecursive(projectName: String, folderId: String) {
        val itemsRef = db.collection("projects").document(projectName).collection("items")
        itemsRef.whereEqualTo("parentFolderId", folderId).get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val childId = doc.id
                val isChildFolder = doc.getBoolean("isFolder") ?: false

                doc.reference.delete()

                if (isChildFolder) {
                    deleteFolderRecursive(projectName, childId)
                }
            }
        }
    }

    private val _comments = MutableStateFlow<List<FileComment>>(emptyList())
    val comments: StateFlow<List<FileComment>> = _comments.asStateFlow()

    private var commentsListener: ListenerRegistration? = null

    fun loadComments(projectName: String, itemId: String) {
        commentsListener?.remove()
        commentsListener = db.collection("projects").document(projectName)
            .collection("items").document(itemId).collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _comments.value = emptyList()
                    return@addSnapshotListener
                }

                val commentList = snapshot.documents.mapNotNull { doc ->
                    val author = doc.getString("author") ?: "Artist"
                    val text = doc.getString("text") ?: ""
                    val time = doc.getLong("timestamp") ?: 0L
                    FileComment(doc.id, author, text, time)
                }
                _comments.value = commentList
            }
    }

    fun addComment(projectName: String, itemId: String, author: String, text: String) {
        if (text.isBlank()) return
        val commentData = mapOf(
            "author" to author,
            "text" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("projects").document(projectName)
            .collection("items").document(itemId).collection("comments")
            .add(commentData)
    }

    fun stopCommentsListener() {
        commentsListener?.remove()
        _comments.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        currentListener?.remove()
        commentsListener?.remove()
    }
}
