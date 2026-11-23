package com.hb.office

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Project(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val driveFolderUrl: String = ""
)

class ProjectViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val projectsCollection = db.collection("projects")

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userRole = MutableStateFlow("user")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userName = MutableStateFlow("Artist")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _showApprovalEvent = MutableStateFlow(false)
    val showApprovalEvent: StateFlow<Boolean> = _showApprovalEvent.asStateFlow()

    private val _isKickedOut = MutableStateFlow(false)
    val isKickedOut: StateFlow<Boolean> = _isKickedOut.asStateFlow()

    init {
        checkUserRoleAndLoadProjects()
    }

    fun resetApprovalEvent() {
        _showApprovalEvent.value = false
    }

    private fun checkUserRoleAndLoadProjects() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _isLoading.value = false
            return
        }

        db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                // Profil wurde gelöscht -> Sofort rausschmeißen (aber Auth-Konto unberührt lassen für späteren Re-Login)
                FirebaseAuth.getInstance().signOut()
                _isKickedOut.value = true
                _userRole.value = "user"
                _projects.value = emptyList()
                _isLoading.value = false
                return@addSnapshotListener
            }

            val role = snapshot.getString("role") ?: "user"
            _userRole.value = role

            val fetchedName = snapshot?.getString("username")?.takeIf { it.isNotBlank() }
                ?: snapshot?.getString("email")?.substringBefore("@")
                ?: "Artist"
            _userName.value = fetchedName

            val allowedProjects = (snapshot?.get("allowedProjects") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()

            val isCurrentlyApproved = role == "admin" || role == "hb" || role == "sr" || allowedProjects.isNotEmpty()

            val appContext = com.google.firebase.FirebaseApp.getInstance().applicationContext
            val prefs = appContext.getSharedPreferences("artist_office_prefs", android.content.Context.MODE_PRIVATE)
            val prefKey = "user_approved_$uid"
            val wasAlreadyNotified = prefs.getBoolean(prefKey, false)

            if (!isCurrentlyApproved) {
                // Wenn Nutzer noch nicht freigeschaltet ist -> Worker starten & Pref zurücksetzen
                ApprovalWorker.schedule(appContext)
                prefs.edit().putBoolean(prefKey, false).apply()
            } else {
                // Sobald freigeschaltet -> Worker stoppen
                ApprovalWorker.cancel(appContext)

                // Nur EIN EINZIGES Mal benachrichtigen!
                if (!wasAlreadyNotified) {
                    _showApprovalEvent.value = true
                    prefs.edit().putBoolean(prefKey, true).apply()
                }
            }

            loadProjects(role, allowedProjects)
        }
    }

    private fun loadProjects(role: String, allowedProjects: List<String>) {
        projectsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                if (snapshot.isEmpty && role == "admin") {
                    // Falls die Projekte-Sammlung ganz leer ist und ein Admin schaut, legen wir die 2 Standardprojekte an
                    val hindenburg = Project("1", "Hindenburg", "Demos, Stems und Master für Hindenburg", "https://drive.google.com/drive/folders/1haja_GarL9Hj-M3c5imeq-C06lLqbZpZ?usp=drive_link")
                    val schuppen = Project("2", "SchuppenRecords", "Label-Verwaltung und Releases", "")

                    projectsCollection.document(hindenburg.id).set(hindenburg)
                    projectsCollection.document(schuppen.id).set(schuppen)
                }

                val projectList = snapshot.documents.mapNotNull { doc ->
                    val proj = doc.toObject(Project::class.java)
                    if (proj == null) return@mapNotNull null

                    // Automatische Aktualisierung des Hindenburg-Projekt-Links auf den neuen Google Drive Ordner-Link
                    val finalProj = if (proj.name.equals("Hindenburg", ignoreCase = true)) {
                        val updated = proj.copy(driveFolderUrl = "https://drive.google.com/drive/folders/1haja_GarL9Hj-M3c5imeq-C06lLqbZpZ?usp=drive_link")
                        projectsCollection.document(doc.id).update("driveFolderUrl", updated.driveFolderUrl)
                        updated
                    } else {
                        proj
                    }

                    val isAllowed = when (role.lowercase()) {
                        "admin" -> true
                        "hb" -> finalProj.name.equals("Hindenburg", ignoreCase = true) || allowedProjects.contains(finalProj.name)
                        "sr" -> finalProj.name.equals("SchuppenRecords", ignoreCase = true) || allowedProjects.contains(finalProj.name)
                        else -> allowedProjects.contains(finalProj.name)
                    }

                    if (isAllowed) finalProj else null
                }
                _projects.value = projectList
            }
            _isLoading.value = false
        }
    }

    fun createProject(name: String, description: String) {
        if (name.isBlank()) return
        val docRef = projectsCollection.document()
        val newProject = Project(id = docRef.id, name = name.trim(), description = description.trim())
        docRef.set(newProject)
    }
}
