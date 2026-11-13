package com.nk.activityreportapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.activityreportapp.domain.model.AppDetail
import com.nk.activityreportapp.domain.usecase.GetAppDetailUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AppDetailUiState(
    val packageName: String,
    val isLoading: Boolean = true,
    val detail: AppDetail? = null,
)

/** The navigation key's arguments arrive through assisted injection. */
@HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
class AppDetailViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    @Assisted weekStartEpochDay: Long,
    getAppDetail: GetAppDetailUseCase,
) : ViewModel() {

    val uiState: StateFlow<AppDetailUiState> =
        getAppDetail(packageName, LocalDate.ofEpochDay(weekStartEpochDay))
            .map { AppDetailUiState(packageName, isLoading = false, detail = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = AppDetailUiState(packageName),
            )

    @AssistedFactory
    interface Factory {
        fun create(packageName: String, weekStartEpochDay: Long): AppDetailViewModel
    }
}
