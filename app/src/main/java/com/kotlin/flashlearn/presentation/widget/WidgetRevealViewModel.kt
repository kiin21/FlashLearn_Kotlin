package com.kotlin.flashlearn.presentation.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.widget.DailyWidgetEngine
import com.kotlin.flashlearn.domain.widget.WidgetState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetRevealViewModel @Inject constructor(
    private val engine: DailyWidgetEngine
) : ViewModel() {

    private val _state = MutableStateFlow<WidgetState>(WidgetState.Exhausted("", "Loading"))
    val state: StateFlow<WidgetState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = launchAction { engine.getState() }
    fun reveal() = launchAction { engine.reveal() }
    fun missed() = launchAction { engine.missed() }
    fun gotIt() = launchAction { engine.gotIt() }

    private fun launchAction(block: suspend () -> WidgetState) {
        viewModelScope.launch {
            if (_busy.value) return@launch
            _busy.value = true
            try {
                _state.value = block()
            } finally {
                _busy.value = false
            }
        }
    }
}
