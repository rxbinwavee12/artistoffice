package com.hb.office

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SocialLinkItem(
    val id: String = "",
    val platform: String = "",
    val url: String = "",
    val timestamp: Long = 0
)

class LinkHubViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _links = MutableStateFlow<List<SocialLinkItem>>(emptyList())
    val links: StateFlow<List<SocialLinkItem>> = _links.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentListener: ListenerRegistration? = null

    fun loadLinks(projectName: String) {
        currentListener?.remove()

        currentListener = db.collection("projects").document(projectName)
            .collection("links")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val platform = doc.getString("platform") ?: "Unbekannt"
                    val url = doc.getString("url") ?: ""
                    val time = doc.getLong("timestamp") ?: 0L

                    SocialLinkItem(
                        id = doc.id,
                        platform = platform,
                        url = url,
                        timestamp = time
                    )
                }

                _links.value = list
                _isLoading.value = false
            }
    }

    fun addLink(projectName: String, platform: String, url: String) {
        if (platform.isBlank() || url.isBlank()) return
        val docRef = db.collection("projects").document(projectName).collection("links").document()
        val item = mapOf(
            "platform" to platform.trim(),
            "url" to url.trim(),
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(item)
    }

    fun deleteLink(projectName: String, id: String) {
        db.collection("projects").document(projectName).collection("links").document(id).delete()
    }

    override fun onCleared() {
        super.onCleared()
        currentListener?.remove()
    }
}
