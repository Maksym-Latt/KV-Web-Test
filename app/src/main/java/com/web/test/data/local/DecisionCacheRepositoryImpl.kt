package com.web.test.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.web.test.domain.model.CachedDecision
import com.web.test.domain.model.DecisionResult
import com.web.test.domain.repository.DecisionCacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.decisionDataStore by preferencesDataStore(name = "decision_store")

class DecisionCacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DecisionCacheRepository {

    private val isModeratorKey = booleanPreferencesKey("is_moderator")
    private val cachedUrlKey = stringPreferencesKey("cached_url")
    private val timestampKey = longPreferencesKey("decision_timestamp")

    override suspend fun getCachedDecision(): CachedDecision? {
        val prefs = context.decisionDataStore.data.map { it }.first()
        val hasModerator = prefs[isModeratorKey]
        val timestamp = prefs[timestampKey] ?: return null

        return CachedDecision(
            isModerator = hasModerator ?: return null,
            cachedUrl = prefs[cachedUrlKey],
            decisionTimestamp = timestamp
        )
    }

    override suspend fun saveDecision(result: DecisionResult) {
        context.decisionDataStore.edit { prefs ->
            prefs[isModeratorKey] = result.isModerator
            prefs[cachedUrlKey] = result.targetUrl.toString()
            prefs[timestampKey] = System.currentTimeMillis()
        }
    }

    override suspend fun clear() {
        context.decisionDataStore.edit { it.clear() }
    }
}