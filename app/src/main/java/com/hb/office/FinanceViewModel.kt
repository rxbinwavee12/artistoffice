package com.hb.office

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FinanceItem(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val isIncome: Boolean = true,
    val category: String = "Sonstiges",
    val timestamp: Long = 0
)

class FinanceViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _finances = MutableStateFlow<List<FinanceItem>>(emptyList())
    val finances: StateFlow<List<FinanceItem>> = _finances.asStateFlow()

    private val _totalBalance = MutableStateFlow(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentListener: ListenerRegistration? = null

    fun loadFinances(projectName: String) {
        currentListener?.remove()

        currentListener = db.collection("projects").document(projectName)
            .collection("finances")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                var balance = 0.0
                val list = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: "Unbenannt"
                    val amount = doc.getDouble("amount") ?: 0.0
                    val isIncome = doc.getBoolean("isIncome") ?: true
                    val category = doc.getString("category") ?: "Sonstiges"
                    val time = doc.getLong("timestamp") ?: 0L

                    if (isIncome) balance += amount else balance -= amount

                    FinanceItem(
                        id = doc.id,
                        title = title,
                        amount = amount,
                        isIncome = isIncome,
                        category = category,
                        timestamp = time
                    )
                }.reversed()

                _finances.value = list
                _totalBalance.value = balance
                _isLoading.value = false
            }
    }

    fun addTransaction(projectName: String, title: String, amount: Double, isIncome: Boolean, category: String) {
        if (title.isBlank() || amount <= 0) return
        val docRef = db.collection("projects").document(projectName).collection("finances").document()
        val item = mapOf(
            "title" to title.trim(),
            "amount" to amount,
            "isIncome" to isIncome,
            "category" to category,
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(item)
    }

    fun deleteTransaction(projectName: String, id: String) {
        db.collection("projects").document(projectName).collection("finances").document(id).delete()
    }

    override fun onCleared() {
        super.onCleared()
        currentListener?.remove()
    }
}
