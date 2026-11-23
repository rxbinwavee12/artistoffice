package com.hb.office

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CalendarEvent(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val type: String = "Probe",
    val notes: String = "",
    val rsvps: Map<String, String> = emptyMap(),
    val timestamp: Long = 0
)

class CalendarViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentListener: ListenerRegistration? = null

    fun loadEvents(projectName: String) {
        currentListener?.remove()

        currentListener = db.collection("projects").document(projectName)
            .collection("events")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: "Unbenannt"
                    val date = doc.getString("date") ?: ""
                    val type = doc.getString("type") ?: "Probe"
                    val notes = doc.getString("notes") ?: ""
                    val rsvps = (doc.get("rsvps") as? Map<*, *>)?.mapNotNull { (k, v) -> k.toString() to v.toString() }?.toMap() ?: emptyMap()
                    val time = doc.getLong("timestamp") ?: 0L

                    CalendarEvent(
                        id = doc.id,
                        title = title,
                        date = date,
                        type = type,
                        notes = notes,
                        rsvps = rsvps,
                        timestamp = time
                    )
                }
                _events.value = list
                _isLoading.value = false
            }
    }

    fun addEvent(projectName: String, title: String, date: String, type: String, notes: String) {
        if (title.isBlank()) return
        val docRef = db.collection("projects").document(projectName).collection("events").document()
        val item = mapOf(
            "title" to title.trim(),
            "date" to date.trim(),
            "type" to type,
            "notes" to notes.trim(),
            "rsvps" to emptyMap<String, String>(),
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(item)
    }

    fun setRsvp(projectName: String, eventId: String, username: String, rsvpStatus: String) {
        db.collection("projects").document(projectName).collection("events").document(eventId).update("rsvps.$username", rsvpStatus)
    }

    fun deleteEvent(projectName: String, id: String) {
        db.collection("projects").document(projectName).collection("events").document(id).delete()
    }

    override fun onCleared() {
        super.onCleared()
        currentListener?.remove()
    }
}
