package com.nk.activityreportapp.presentation.viewmodel

import com.nk.activityreportapp.domain.model.PermissionState
import com.nk.activityreportapp.domain.repository.FakePermissionRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {
    private val repository = FakePermissionRepository()
    private val viewModel = MainViewModel(repository)

    @Test
    fun `initial permissions are loaded from repository`() {
        val expected = PermissionState(usageAccess = true, notificationAccess = false)
        repository.state = expected
        
        // Create new VM to pick up initial state
        val vm = MainViewModel(repository)
        assertEquals(expected, vm.permissions.value)
    }

    @Test
    fun `refreshPermissions updates state from repository`() {
        val initialState = PermissionState(usageAccess = false, notificationAccess = false)
        repository.state = initialState
        assertEquals(initialState, viewModel.permissions.value)

        val updatedState = PermissionState(usageAccess = true, notificationAccess = true)
        repository.state = updatedState
        viewModel.refreshPermissions()

        assertEquals(updatedState, viewModel.permissions.value)
    }
}
