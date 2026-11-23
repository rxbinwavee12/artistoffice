package com.hb.office

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PollItem(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val votes: Map<String, Int> = emptyMap(), // username -> option index
    val timestamp: Long = 0
)

class PollViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _polls = MutableStateFlow<List<PollItem>>(emptyList())
    val polls: StateFlow<List<PollItem>> = _polls.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentListener: ListenerRegistration? = null

    fun loadPolls(projectName: String) {
        currentListener?.remove()

        currentListener = db.collection("projects").document(projectName)
            .collection("polls")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val question = doc.getString("question") ?: "Unbekannte Frage"
                    val options = (doc.get("options") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                    val votesMap = (doc.get("votes") as? Map<*, *>)?.mapNotNull {
                        val k = it.key.toString()
                        val v = it.value.toString().toIntOrNull()
                        if (v != null) k to v else null
                    }?.toMap() ?: emptyMap()
                    val time = doc.getLong("timestamp") ?: 0L

                    PollItem(
                        id = doc.id,
                        question = question,
                        options = options,
                        votes = votesMap,
                        timestamp = time
                    )
                }.reversed() // Neueste oben

                _polls.value = list
                _isLoading.value = false
            }
    }

    fun createPoll(projectName: String, question: String, options: List<String>) {
        if (question.isBlank() || options.isEmpty()) return
        val cleanOptions = options.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanOptions.isEmpty()) return

        val docRef = db.collection("projects").document(projectName).collection("polls").document()
        val item = mapOf(
            "question" to question.trim(),
            "options" to cleanOptions,
            "votes" to emptyMap<String, Int>(),
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(item)
    }

    fun castVote(projectName: String, pollId: String, username: String, optionIndex: Int) {
        if (username.isBlank()) return
        db.collection("projects").document(projectName).collection("polls").document(pollId)
            .update("votes.$username", optionIndex)
    }

    fun deletePoll(projectName: String, pollId: String) {
        db.collection("projects").document(projectName).collection("polls").document(pollId).delete()
    }

    override fun onCleared() {
        super.onCleared()
        currentListener?.remove()
    }
}
