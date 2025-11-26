package com.web.test.domain.usecase

import com.web.test.domain.model.CachedDecision
import com.web.test.domain.repository.DecisionStorage
import javax.inject.Inject

class GetCachedDecisionUseCase @Inject constructor(
    private val storage: DecisionStorage,
) {
    suspend operator fun invoke(): CachedDecision? = storage.getCachedDecision()
}
