package com.web.test.domain.usecase

import com.web.test.domain.model.CloakInfo
import com.web.test.domain.repository.CloakInfoRepository
import javax.inject.Inject

class CollectCloakInfoUseCase @Inject constructor(
    private val repository: CloakInfoRepository
) {
    suspend operator fun invoke(): CloakInfo = repository.collectCloakInfo()
}
