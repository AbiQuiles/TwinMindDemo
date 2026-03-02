package com.aquiles.twinminddemo.data.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class SessionStateHolder @Inject constructor() {
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId

    fun setSessionId(id: String) {
        _currentSessionId.value = id
    }
}