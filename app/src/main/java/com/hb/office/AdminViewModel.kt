package com.hb.office

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "user",
    val allowedProjects: List<String> = emptyList()
)

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users: StateFlow<List<UserProfile>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAllUsers()
    }

    private fun loadAllUsers() {
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                _isLoading.value = false
                return@addSnapshotListener
            }

            val userList = snapshot.documents.mapNotNull { doc ->
                val email = doc.getString("email") ?: ""
                val username = doc.getString("username") ?: ""
                val role = doc.getString("role") ?: "user"
                val allowedProjects = (doc.get("allowedProjects") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()

                UserProfile(
                    uid = doc.id,
                    email = email,
                    username = username,
                    role = role,
                    allowedProjects = allowedProjects
                )
            }.sortedBy { it.email }

            _users.value = userList
            _isLoading.value = false
        }
    }

    fun updateUser(uid: String, username: String, role: String, allowedProjects: List<String>) {
        val userData = mapOf(
            "username" to username.trim(),
            "role" to role,
            "allowedProjects" to allowedProjects
        )
        db.collection("users").document(uid).update(userData)
    }
}
