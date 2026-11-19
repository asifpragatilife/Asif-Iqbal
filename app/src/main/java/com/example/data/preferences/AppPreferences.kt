package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.asifDataStore: DataStore<Preferences> by preferencesDataStore(name = "asifaraf_prefs")

enum class ScanQuality(val label: String, val jpegQuality: Int, val maxDimension: Int) {
    STANDARD("Standard", 78, 1400),
    HIGH("High", 90, 2000),
    ULTRA("Ultra", 96, 2800),
}

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    BN("bn", "বাংলা (Bangla)"),
}

data class UserPreferencesState(
    val onboardingCompleted: Boolean = false,
    val isDarkTheme: Boolean = true,
    val language: AppLanguage = AppLanguage.EN,
    val defaultScanQuality: ScanQuality = ScanQuality.HIGH,
    val autoLockSeconds: Int = 60,
    val biometricEnabled: Boolean = true,
    val pinHash: String = "",
    val pinSalt: String = "",
    val failedPinAttempts: Int = 0,
    val lockoutUntilMs: Long = 0L,
    val secureCloudOptIn: Boolean = false,
    val googleDriveOptIn: Boolean = false,
    val dropboxOptIn: Boolean = false,
    val lastSyncedMs: Long = 0L,
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val SCAN_QUALITY = stringPreferencesKey("scan_quality")
        val AUTO_LOCK_SEC = intPreferencesKey("auto_lock_sec")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val FAILED_PIN = intPreferencesKey("failed_pin")
        val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
        val SECURE_CLOUD = booleanPreferencesKey("secure_cloud_opt_in")
        val GOOGLE_DRIVE = booleanPreferencesKey("google_drive_opt_in")
        val DROPBOX = booleanPreferencesKey("dropbox_opt_in")
        val LAST_SYNCED = longPreferencesKey("last_synced_ms")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.asifDataStore.data.map { prefs ->
        UserPreferencesState(
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            language = if (prefs[Keys.LANGUAGE] == "bn") AppLanguage.BN else AppLanguage.EN,
            defaultScanQuality = ScanQuality.entries.firstOrNull {
                it.name == prefs[Keys.SCAN_QUALITY]
            } ?: ScanQuality.HIGH,
            autoLockSeconds = prefs[Keys.AUTO_LOCK_SEC] ?: 60,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: true,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            failedPinAttempts = prefs[Keys.FAILED_PIN] ?: 0,
            lockoutUntilMs = prefs[Keys.LOCKOUT_UNTIL] ?: 0L,
            secureCloudOptIn = prefs[Keys.SECURE_CLOUD] ?: false,
            googleDriveOptIn = prefs[Keys.GOOGLE_DRIVE] ?: false,
            dropboxOptIn = prefs[Keys.DROPBOX] ?: false,
            lastSyncedMs = prefs[Keys.LAST_SYNCED] ?: 0L,
        )
    }

    suspend fun current(): UserPreferencesState = preferencesFlow.first()

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.asifDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.asifDataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.asifDataStore.edit { it[Keys.LANGUAGE] = lang.code }
    }

    suspend fun setScanQuality(quality: ScanQuality) {
        context.asifDataStore.edit { it[Keys.SCAN_QUALITY] = quality.name }
    }

    suspend fun setAutoLockSeconds(seconds: Int) {
        context.asifDataStore.edit { it[Keys.AUTO_LOCK_SEC] = seconds }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.asifDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun savePinCredentials(hash: String, salt: String) {
        context.asifDataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_SALT] = salt
            it[Keys.FAILED_PIN] = 0
            it[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun recordFailedPin(attempts: Int, lockoutUntilMs: Long) {
        context.asifDataStore.edit {
            it[Keys.FAILED_PIN] = attempts
            it[Keys.LOCKOUT_UNTIL] = lockoutUntilMs
        }
    }

    suspend fun resetPinFailures() {
        context.asifDataStore.edit {
            it[Keys.FAILED_PIN] = 0
            it[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun setSecureCloudOptIn(enabled: Boolean) {
        context.asifDataStore.edit {
            it[Keys.SECURE_CLOUD] = enabled
            if (enabled) it[Keys.LAST_SYNCED] = System.currentTimeMillis()
        }
    }

    suspend fun setGoogleDriveOptIn(enabled: Boolean) {
        context.asifDataStore.edit { it[Keys.GOOGLE_DRIVE] = enabled }
    }

    suspend fun setDropboxOptIn(enabled: Boolean) {
        context.asifDataStore.edit { it[Keys.DROPBOX] = enabled }
    }

    suspend fun clearAll() {
        context.asifDataStore.edit { it.clear() }
    }
}
