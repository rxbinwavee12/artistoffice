package com.hb.office

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SetlistItem(
    val id: String = "",
    val name: String = "",
    val songs: List<String> = emptyList(),
    val notes: String = "",
    val timestamp: Long = 0
)

class SetlistViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _setlists = MutableStateFlow<List<SetlistItem>>(emptyList())
    val setlists: StateFlow<List<SetlistItem>> = _setlists.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentListener: ListenerRegistration? = null

    fun loadSetlists(projectName: String) {
        currentListener?.remove()

        currentListener = db.collection("projects").document(projectName)
            .collection("setlists")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: "Unbenannt"
                    val songs = (doc.get("songs") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                    val notes = doc.getString("notes") ?: ""
                    val time = doc.getLong("timestamp") ?: 0L

                    SetlistItem(
                        id = doc.id,
                        name = name,
                        songs = songs,
                        notes = notes,
                        timestamp = time
                    )
                }
                _setlists.value = list
                _isLoading.value = false
            }
    }

    fun createSetlist(projectName: String, name: String, songs: List<String>, notes: String) {
        if (name.isBlank()) return
        val docRef = db.collection("projects").document(projectName).collection("setlists").document()
        val item = mapOf(
            "name" to name.trim(),
            "songs" to songs.map { it.trim() }.filter { it.isNotBlank() },
            "notes" to notes.trim(),
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(item)
    }

    fun deleteSetlist(projectName: String, id: String) {
        db.collection("projects").document(projectName).collection("setlists").document(id).delete()
    }

    override fun onCleared() {
        super.onCleared()
        currentListener?.remove()
    }
}
