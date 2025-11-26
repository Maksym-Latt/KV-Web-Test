package com.web.test.domain.repository

import com.web.test.domain.model.CloakInfo

interface CloakInfoRepository {
    suspend fun collectCloakInfo(): CloakInfo
}
