package com.web.test.domain.repository

import com.web.test.domain.model.CloakInfo

interface CloakInfoProvider {
    suspend fun collect(): CloakInfo
}
