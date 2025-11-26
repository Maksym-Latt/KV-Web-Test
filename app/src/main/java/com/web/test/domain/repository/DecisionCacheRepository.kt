package com.web.test.domain.repository

import com.web.test.domain.model.CachedDecision
import com.web.test.domain.model.DecisionResult

interface DecisionCacheRepository {
    suspend fun getCachedDecision(): CachedDecision?
    suspend fun saveDecision(result: DecisionResult)
    suspend fun clear()
}
