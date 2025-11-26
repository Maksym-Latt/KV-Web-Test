package com.web.test.domain.usecase

import com.web.test.domain.model.CloakInfo
import com.web.test.domain.repository.CloakInfoProvider
import javax.inject.Inject

class CollectCloakInfoUseCase @Inject constructor(
    private val cloakInfoProvider: CloakInfoProvider,
) {
    suspend operator fun invoke(): CloakInfo = cloakInfoProvider.collect()
}
