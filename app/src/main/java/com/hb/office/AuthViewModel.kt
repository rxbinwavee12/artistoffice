package com.hb.office

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        val db = FirebaseFirestore.getInstance()
                        db.collection("users").document(user.uid).get().addOnSuccessListener { snapshot ->
                            if (!snapshot.exists()) {
                                // Wurde gelöscht -> Als komplett neuen, gesperrten Nutzer re-initialisieren
                                ensureUserDocumentExists(user.uid, user.email ?: email)
                            }
                            _authState.value = AuthState.Success
                        }.addOnFailureListener {
                            _authState.value = AuthState.Success
                        }
                    } else {
                        _authState.value = AuthState.Success
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Login fehlgeschlagen")
                }
            }
    }

    fun register(email: String, pass: String) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        val db = FirebaseFirestore.getInstance()
                        val userData = mapOf(
                            "email" to email,
                            "username" to email.substringBefore("@"),
                            "role" to "user",
                            "allowedProjects" to emptyList<String>()
                        )
                        db.collection("users").document(user.uid).set(userData).addOnCompleteListener {
                            _authState.value = AuthState.Success
                        }
                    } else {
                        _authState.value = AuthState.Success
                    }
                } else {
                    val ex = task.exception
                    if (ex?.message?.contains("already in use", ignoreCase = true) == true) {
                        _authState.value = AuthState.Error("Diese E-Mail ist bereits registriert. Bitte nutze stattdessen den Login.")
                    } else {
                        _authState.value = AuthState.Error(ex?.message ?: "Registrierung fehlgeschlagen")
                    }
                }
            }
    }

    private fun ensureUserDocumentExists(uid: String, email: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)
        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val userData = mapOf(
                    "email" to email,
                    "username" to email.substringBefore("@"),
                    "role" to "user",
                    "allowedProjects" to emptyList<String>()
                )
                userRef.set(userData)
            }
        }
    }

    fun checkUser() {
        val user = auth.currentUser
        if (user != null) {
            // Lade den aktuellsten Status vom Server (deckt ab, wenn der Account gelöscht wurde)
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                } else {
                    // Account existiert nicht mehr oder Fehler
                    auth.signOut()
                    _authState.value = AuthState.Idle
                }
            }
        } else {
            _authState.value = AuthState.Idle
        }
    }
    
    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
