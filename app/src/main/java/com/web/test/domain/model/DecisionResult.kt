package com.web.test.domain.model

data class DecisionResult(
    val isModerator: Boolean,
    val targetUrl: String?,
    val reason: String? = null,
)
