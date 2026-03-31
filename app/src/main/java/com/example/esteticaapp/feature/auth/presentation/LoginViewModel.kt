package com.example.esteticaapp.feature.auth.presentation

import android.content.Context
import android.util.Log
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
import com.example.esteticaapp.R

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
                
                // Fallback manual to avoid compilation error if R.string.default_web_client_id is missing
                val webClientId = "797437624053-h95fnd559c6gt3f5qjh8n3lmi7ghv1pl.apps.googleusercontent.com"

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

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
                Log.e("LoginViewModel", "Google Sign In Error: ${e.message}", e)
                val errorMessage = when {
                    e is GetCredentialCancellationException -> "Inicio de sesión cancelado."
                    e is NoCredentialException || e.message?.contains("No credentials available") == true -> 
                        "No sincronizó con Google. Asegúrate de haber agregado la firma SHA-1 correcta en Firebase y haber actualizado el google-services.json."
                    else -> "Error de configuración: ${e.localizedMessage}"
                }
                _loginState.value = LoginUiState.Error(errorMessage)
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Unexpected Error: ${e.message}", e)
                _loginState.value = LoginUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    private suspend fun checkUserExists(uid: String, email: String?) {
        try {
            if (AdminConfig.isAdmin(email)) {
                _loginState.value = LoginUiState.Success(navigateTo = "admin_welcome")
                return
            }

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
