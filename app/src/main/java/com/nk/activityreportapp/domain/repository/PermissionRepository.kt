package com.nk.activityreportapp.domain.repository

import com.nk.activityreportapp.domain.model.PermissionState

interface PermissionRepository {
    fun currentState(): PermissionState
}
