package com.steptracker.app.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_preferences")

@Singleton
class SecurePreferences @Inject constructor(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    
    companion object {
        private val API_KEYS = stringPreferencesKey("encrypted_api_keys")
        private val USER_TOKEN = stringPreferencesKey("encrypted_user_token")
        private val REFRESH_TOKEN = stringPreferencesKey("encrypted_refresh_token")
        private val USER_CREDENTIALS = stringPreferencesKey("encrypted_user_credentials")
    }
    
    suspend fun storeApiKey(keyName: String, apiKey: String) {
        val encryptedKey = securityManager.encryptData(apiKey)
        context.secureDataStore.edit { preferences ->
            val currentKeys = preferences[API_KEYS] ?: "{}"
            val keysMap = parseKeysMap(currentKeys)
            keysMap[keyName] = encryptedKey
            preferences[API_KEYS] = serializeKeysMap(keysMap)
        }
    }
    
    suspend fun getApiKey(keyName: String): String? {
        val keysJson = context.secureDataStore.data.map { it[API_KEYS] }.first()
        val keysMap = parseKeysMap(keysJson ?: "{}")
        val encryptedKey = keysMap[keyName] ?: return null
        return try {
            securityManager.decryptData(encryptedKey)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun storeUserToken(token: String) {
        val encryptedToken = securityManager.encryptData(token)
        context.secureDataStore.edit { preferences ->
            preferences[USER_TOKEN] = encryptedToken
        }
    }
    
    suspend fun getUserToken(): String? {
        val encryptedToken = context.secureDataStore.data.map { it[USER_TOKEN] }.first()
        return encryptedToken?.let { 
            try {
                securityManager.decryptData(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun storeRefreshToken(token: String) {
        val encryptedToken = securityManager.encryptData(token)
        context.secureDataStore.edit { preferences ->
            preferences[REFRESH_TOKEN] = encryptedToken
        }
    }
    
    suspend fun getRefreshToken(): String? {
        val encryptedToken = context.secureDataStore.data.map { it[REFRESH_TOKEN] }.first()
        return encryptedToken?.let { 
            try {
                securityManager.decryptData(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun storeUserCredentials(email: String, password: String) {
        val credentials = "$email:$password"
        val encryptedCredentials = securityManager.encryptData(credentials)
        context.secureDataStore.edit { preferences ->
            preferences[USER_CREDENTIALS] = encryptedCredentials
        }
    }
    
    suspend fun getUserCredentials(): Pair<String, String>? {
        val encryptedCredentials = context.secureDataStore.data.map { it[USER_CREDENTIALS] }.first()
        return encryptedCredentials?.let { 
            try {
                val decrypted = securityManager.decryptData(it)
                val parts = decrypted.split(":")
                if (parts.size == 2) {
                    Pair(parts[0], parts[1])
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun clearAllSecureData() {
        context.secureDataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    private fun parseKeysMap(json: String): MutableMap<String, String> {
        return try {
            // Simple JSON parsing for key-value pairs
            val map = mutableMapOf<String, String>()
            val trimmed = json.trim('{', '}')
            if (trimmed.isNotEmpty()) {
                trimmed.split(",").forEach { pair ->
                    val keyValue = pair.split(":")
                    if (keyValue.size == 2) {
                        val key = keyValue[0].trim('"')
                        val value = keyValue[1].trim('"')
                        map[key] = value
                    }
                }
            }
            map
        } catch (e: Exception) {
            mutableMapOf()
        }
    }
    
    private fun serializeKeysMap(map: Map<String, String>): String {
        return map.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }.let { "{$it}" }
    }
} 