package com.example.esteticaapp.feature.auth.presentation

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esteticaapp.core.config.AdminConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState = _loginState.asStateFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            try {
                val credentialManager = CredentialManager.create(context)
                
                // Web Client ID (tipo 3) obtenido de google-services.json
                val webClientId = "797437624053-h95fnd559c6gt3f5qjh8n3lmi7ghv1pl.apps.googleusercontent.com" 

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                // Usamos GoogleIdTokenCredential.createFrom(credential.data) para parsear el token correctamente
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                if (user != null) {
                    checkUserExists(user.uid, user.email)
                } else {
                    _loginState.value = LoginUiState.Error("Error al obtener el usuario de Firebase")
                }
                
            } catch (e: GetCredentialException) {
                val errorMessage = when {
                    e is GetCredentialCancellationException -> "Inicio de sesión cancelado."
                    e is NoCredentialException || e.message?.contains("No credentials available") == true -> 
                        "No sincronizó con Google. Verifica tu conexión a internet."
                    else -> "Error de autenticación con Google: ${e.localizedMessage}"
                }
                _loginState.value = LoginUiState.Error(errorMessage)
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    private suspend fun checkUserExists(uid: String, email: String?) {
        try {
            // Verificar si es administrador consultando Firestore
            if (AdminConfig.isAdmin(email)) {
                _loginState.value = LoginUiState.Success(navigateTo = "admin_welcome")
                return
            }

            // Buscar el uid en la colección 'clientes' de Firestore
            val document = db.collection("clientes").document(uid).get().await()
            if (document.exists()) {
                _loginState.value = LoginUiState.Success(navigateTo = "main")
            } else {
                _loginState.value = LoginUiState.Success(navigateTo = "register")
            }
        } catch (e: Exception) {
            _loginState.value = LoginUiState.Error("Error al verificar perfil en Firestore: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _loginState.value = LoginUiState.Idle
    }
}

