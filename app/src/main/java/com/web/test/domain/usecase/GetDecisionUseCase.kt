package com.web.test.domain.usecase

import com.web.test.domain.model.DecisionInput
import com.web.test.domain.model.DecisionResult
import com.web.test.domain.repository.DecisionRepository
import javax.inject.Inject

class GetDecisionUseCase @Inject constructor(
    private val repository: DecisionRepository,
) {
    suspend operator fun invoke(input: DecisionInput): DecisionResult = repository.getDecision(input)
}
