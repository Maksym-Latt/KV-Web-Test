package com.web.test.data.decision

import com.web.test.domain.model.DecisionInput
import com.web.test.domain.model.DecisionResult
import com.web.test.domain.repository.DecisionRepository
import javax.inject.Inject

class LocalDecisionRepository @Inject constructor() : DecisionRepository {
    override suspend fun getDecision(input: DecisionInput): DecisionResult {
        val cloak = input.cloakInfo

        return if (cloak.isUsbDebuggingEnabled) {
            DecisionResult(
                isModerator = false,
                targetUrl = getTestUrl("GOOGLE"),
                reason = "USB debugging enabled"
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


private fun getTestUrl(name: String): String {
    return TestSite.values()
        .firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?.url
        ?: "https://www.google.com"
}

enum class TestSite(val url: String) {

    GOOGLE("https://www.google.com"),
    MSSG("https://www.mssg.me/uk"),
    GGBET("https://www.ggbet.ua"),
    VEGAS("https://vegas-x.net"),
    FILE_SHARING("https://www.mediafire.com"),
    TEST_PAYMENT("https://savelife.in.ua"),
    ROZETKA("https://rozetka.com.ua"),
    TESTING("https://wv-test-panel.space"),
}