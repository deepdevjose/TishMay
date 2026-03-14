package com.example.esteticaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val doc = db.collection("clientes").document(user.uid).get().await()
                    val firstName = doc.getString("firstName") ?: ""
                    _uiState.value = _uiState.value.copy(
                        userName = firstName,
                        isLoading = false
                    )
                } catch (e: Exception) {
                    // Manejo básico de errores, podríamos agregar un campo error si fuera necesario
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

data class HomeUiState(
    val userName: String = "",
    val isLoading: Boolean = true
)

