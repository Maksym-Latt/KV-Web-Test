package com.web.test.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.web.test.domain.model.CachedDecision
import com.web.test.domain.model.DecisionResult
import com.web.test.domain.repository.DecisionStorage
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DecisionPreferenceStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : DecisionStorage {

    override suspend fun saveDecision(decision: DecisionResult, timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_MODERATOR] = decision.isModerator
            decision.targetUrl?.let { prefs[KEY_URL] = it } ?: prefs.remove(KEY_URL)
            prefs[KEY_TIMESTAMP] = timestamp
        }
    }

    override suspend fun getCachedDecision(): CachedDecision? {
        val prefs = dataStore.data.first()
        val hasModerator = prefs[KEY_IS_MODERATOR]
        val timestamp = prefs[KEY_TIMESTAMP]
        return if (hasModerator != null && timestamp != null) {
            CachedDecision(
                isModerator = hasModerator,
                cachedUrl = prefs[KEY_URL],
                decisionTimestamp = timestamp,
            )
        } else {
            null
        }
    }

    private companion object {
        val KEY_IS_MODERATOR = booleanPreferencesKey("isModerator")
        val KEY_URL = stringPreferencesKey("cachedUrl")
        val KEY_TIMESTAMP = longPreferencesKey("decisionTimestamp")
    }
}
