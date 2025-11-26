package com.web.test.domain.usecase

import com.web.test.domain.model.DecisionResult
import com.web.test.domain.repository.DecisionStorage
import javax.inject.Inject

class SaveDecisionUseCase @Inject constructor(
    private val storage: DecisionStorage,
) {
    suspend operator fun invoke(decision: DecisionResult, timestamp: Long) {
        storage.saveDecision(decision, timestamp)
    }
}
