package com.kotlin.flashlearn.presentation.dailyword_archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.DailyWordArchiveItem
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.usecase.GetDailyWordArchiveUseCase
import com.kotlin.flashlearn.utils.DateKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyWordArchiveState(
    val isLoading: Boolean = false,
    val query: String = "",
    val fromDate: String? = null,
    val toDate: String? = null,
    val allItems: List<DailyWordArchiveItem> = emptyList(),
    val items: List<DailyWordArchiveItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DailyWordArchiveViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getArchive: GetDailyWordArchiveUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DailyWordArchiveState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        applyLocalFilter()
    }

    fun setFromDateMillis(millis: Long?) {
        val key = millis?.let { DateKey.fromMillis(it) }
        _state.value = _state.value.copy(fromDate = key)
        refresh()
    }

    fun setToDateMillis(millis: Long?) {
        val key = millis?.let { DateKey.fromMillis(it) }
        _state.value = _state.value.copy(toDate = key)
        refresh()
    }

    fun clearFilters() {
        _state.value = _state.value.copy(fromDate = null, toDate = null, query = "")
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val user = authRepository.getSignedInUser()
            if (user == null) {
                _state.value = _state.value.copy(error = "Please sign in")
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, error = null)

            val items = getArchive(user.userId, _state.value.fromDate, _state.value.toDate)

            _state.value = _state.value.copy(
                isLoading = false,
                allItems = items,
                items = items
            )

            applyLocalFilter()
        }
    }

    private fun applyLocalFilter() {
        val s = _state.value
        val q = s.query.trim().lowercase()

        val base = s.allItems

        if (q.isEmpty()) {
            _state.value = s.copy(items = base)
            return
        }

        val filtered = base.filter {
            it.word.lowercase().contains(q) ||
                    (it.meaning?.lowercase()?.contains(q) == true) ||
                    (it.ipa?.lowercase()?.contains(q) == true)
        }

        _state.value = s.copy(items = filtered)
    }
}

