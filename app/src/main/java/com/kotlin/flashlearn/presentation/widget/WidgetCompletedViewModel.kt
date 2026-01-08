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
class WidgetCompletedViewModel @Inject constructor(
    private val engine: DailyWidgetEngine
) : ViewModel() {

    private val _state = MutableStateFlow<WidgetState>(WidgetState.Exhausted("", "Loading"))
    val state: StateFlow<WidgetState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = engine.getState()
        }
    }
}