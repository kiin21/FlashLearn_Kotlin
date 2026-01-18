package com.kotlin.flashlearn.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.data.local.dao.UserStreakDao
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.widget.WidgetDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeStreakViewModel @Inject constructor(
    authRepository: AuthRepository,
    userStreakDao: UserStreakDao
) : ViewModel() {

    private val userId: String? = authRepository.getSignedInUser()?.userId

    val streakDays: StateFlow<Int> =
        if (userId.isNullOrBlank()) {
            kotlinx.coroutines.flow.flowOf(0).stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        } else {
            userStreakDao.observe(userId)
                .map { streak ->
                    if (streak == null) return@map 0

                    val today = WidgetDate.today()
                    val yesterday = WidgetDate.yesterday(today)
                    val last = streak.lastActiveDate

                    if (last == null) 0
                    else if (last == today || last == yesterday) streak.current
                    else 0
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        }
}