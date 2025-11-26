package com.web.test.domain.model

data class CachedDecision(
    val isModerator: Boolean,
    val cachedUrl: String?,
    val decisionTimestamp: Long,
)
