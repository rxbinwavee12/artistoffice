package com.hb.office

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MerchItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val sizeS: Int = 0,
    val sizeM: Int = 0,
    val sizeL: Int = 0,
    val sizeXL: Int = 0,
    val timestamp: Long = 0
)

class MerchViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _merchList = MutableStateFlow<List<MerchItem>>(emptyList())
    val merchList: StateFlow<List<MerchItem>> = _merchList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentListener: ListenerRegistration? = null

    fun loadMerch(projectName: String) {
        currentListener?.remove()

        currentListener = db.collection("projects").document(projectName)
            .collection("merch")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: "Unbenannt"
                    val price = doc.getDouble("price") ?: 0.0
                    val s = doc.getLong("sizeS")?.toInt() ?: 0
                    val m = doc.getLong("sizeM")?.toInt() ?: 0
                    val l = doc.getLong("sizeL")?.toInt() ?: 0
                    val xl = doc.getLong("sizeXL")?.toInt() ?: 0
                    val time = doc.getLong("timestamp") ?: 0L

                    MerchItem(
                        id = doc.id,
                        name = name,
                        price = price,
                        sizeS = s,
                        sizeM = m,
                        sizeL = l,
                        sizeXL = xl,
                        timestamp = time
                    )
                }
                _merchList.value = list
                _isLoading.value = false
            }
    }

    fun addMerchItem(projectName: String, name: String, price: Double, sizeS: Int, sizeM: Int, sizeL: Int, sizeXL: Int) {
        if (name.isBlank()) return
        val docRef = db.collection("projects").document(projectName).collection("merch").document()
        val item = mapOf(
            "name" to name.trim(),
            "price" to price,
            "sizeS" to sizeS,
            "sizeM" to sizeM,
            "sizeL" to sizeL,
            "sizeXL" to sizeXL,
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(item)
    }

    fun updateStock(projectName: String, id: String, field: String, newCount: Int) {
        if (newCount < 0) return
        db.collection("projects").document(projectName).collection("merch").document(id).update(field, newCount)
    }

    fun deleteMerch(projectName: String, id: String) {
        db.collection("projects").document(projectName).collection("merch").document(id).delete()
    }

    override fun onCleared() {
        super.onCleared()
        currentListener?.remove()
    }
}
