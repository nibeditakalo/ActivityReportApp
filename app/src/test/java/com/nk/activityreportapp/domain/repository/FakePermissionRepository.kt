package com.nk.activityreportapp.domain.repository

import com.nk.activityreportapp.domain.model.PermissionState

class FakePermissionRepository : PermissionRepository {
    var state = PermissionState(usageAccess = false, notificationAccess = false)

    override fun currentState(): PermissionState = state
}
