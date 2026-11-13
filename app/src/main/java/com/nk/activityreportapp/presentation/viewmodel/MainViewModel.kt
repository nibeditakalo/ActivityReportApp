package com.nk.activityreportapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.nk.activityreportapp.domain.model.PermissionState
import com.nk.activityreportapp.domain.repository.PermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository,
) : ViewModel() {

    private val _permissions = MutableStateFlow(permissionRepository.currentState())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = permissionRepository.currentState()
    }
}
