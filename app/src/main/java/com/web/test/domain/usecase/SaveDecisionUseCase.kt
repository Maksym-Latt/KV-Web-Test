package com.web.test.domain.usecase

import com.web.test.domain.model.DecisionResult
import com.web.test.domain.repository.DecisionCacheRepository
import javax.inject.Inject

class SaveDecisionUseCase @Inject constructor(
    private val cacheRepository: DecisionCacheRepository
) {
    suspend operator fun invoke(result: DecisionResult) {
        cacheRepository.saveDecision(result)
    }
}
