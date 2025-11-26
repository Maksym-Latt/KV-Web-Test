package com.web.test.data.decision

import com.web.test.domain.model.DecisionInput
import com.web.test.domain.model.DecisionResult
import com.web.test.domain.repository.DecisionRepository
import javax.inject.Inject

class LocalDecisionRepository @Inject constructor() : DecisionRepository {
    override suspend fun getDecision(input: DecisionInput): DecisionResult {
        val cloak = input.cloakInfo
        val suspicious = cloak.isEmulator || cloak.isRooted || cloak.isVpnEnabled || cloak.hasProxy

        return if (suspicious) {
            DecisionResult(
                isModerator = false,
                targetUrl = "https://example-casino.com",
                reason = "Suspicious environment"
            )
        } else {
            DecisionResult(
                isModerator = true,
                targetUrl = null,
                reason = "Clean device => moderator"
            )
        }
    }
}
