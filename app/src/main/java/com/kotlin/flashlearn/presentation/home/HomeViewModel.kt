package com.kotlin.flashlearn.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.widget.DailyWidgetEngine
import com.kotlin.flashlearn.domain.widget.WidgetState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val widgetEngine: DailyWidgetEngine
) : ViewModel() {

    fun onStreakClick(
        onGoToDailyWidget: () -> Unit,
        onGoToDailyWidgetComplete: () -> Unit
    ) {
        viewModelScope.launch {
            when (widgetEngine.getState()) {
                is WidgetState.DoneToday -> onGoToDailyWidgetComplete()
                else -> onGoToDailyWidget()
            }
        }
    }
}