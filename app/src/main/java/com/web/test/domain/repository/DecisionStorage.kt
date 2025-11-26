package com.web.test.domain.repository

import com.web.test.domain.model.CachedDecision
import com.web.test.domain.model.DecisionResult

interface DecisionStorage {
    suspend fun saveDecision(decision: DecisionResult, timestamp: Long)
    suspend fun getCachedDecision(): CachedDecision?
}
