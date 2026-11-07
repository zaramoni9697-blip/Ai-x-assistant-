package com.jarvis.ai.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.data.repository.AuthRepository
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val serverUrl: String = Graph.session.apiBaseUrl,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val loggedIn: Boolean = false
)

class AuthViewModel(
    private val repo: AuthRepository = Graph.authRepo
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun update(transform: (AuthUiState) -> AuthUiState) {
        _state.value = transform(_state.value)
    }

    fun login() {
        val s = _state.value
        Graph.session.apiBaseUrl = s.serverUrl
        viewModelScope.launch {
            _state.value = s.copy(loading = true, error = null)
            try {
                repo.login(s.email, s.password)
                _state.value = _state.value.copy(loading = false, loggedIn = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = httpMessage(e))
            }
        }
    }

    fun signup() {
        val s = _state.value
        Graph.session.apiBaseUrl = s.serverUrl
        viewModelScope.launch {
            _state.value = s.copy(loading = true, error = null)
            try {
                repo.signup(s.email, s.password, s.displayName)
                _state.value = _state.value.copy(loading = false, loggedIn = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = httpMessage(e))
            }
        }
    }
}
