package com.web.test.domain.usecase

import com.web.test.domain.model.CachedDecision
import com.web.test.domain.repository.DecisionCacheRepository
import javax.inject.Inject

class GetCachedDecisionUseCase @Inject constructor(
    private val cacheRepository: DecisionCacheRepository
) {
    suspend operator fun invoke(): CachedDecision? = cacheRepository.getCachedDecision()
}
