package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.repository.DocumentRepository
import com.example.data.seed.SeedDataManager
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(application) }
    val preferences: AppPreferences by lazy { AppPreferences(application) }
    val cryptoManager: VaultCryptoManager by lazy { VaultCryptoManager(application) }
    val repository: DocumentRepository by lazy {
        DocumentRepository(
            context = application,
            db = database,
            preferences = preferences,
            crypto = cryptoManager,
        )
    }
}

class AsifArafApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            SeedDataManager.seedIfNeeded(
                context = this@AsifArafApplication,
                db = container.database,
                preferences = container.preferences,
                crypto = container.cryptoManager,
            )
        }
    }
}
