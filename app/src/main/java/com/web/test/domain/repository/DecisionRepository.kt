package com.web.test.domain.repository

import com.web.test.domain.model.DecisionInput
import com.web.test.domain.model.DecisionResult

interface DecisionRepository {
    suspend fun getDecision(input: DecisionInput): DecisionResult
}
