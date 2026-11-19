package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.AppLanguage
import com.example.ui.i18n.BanglaStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.navigation.AsifArafNavHost
import com.example.ui.theme.AsifArafTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val prefs by mainViewModel.preferencesState.collectAsStateWithLifecycle()
            val strings = if (prefs.language == AppLanguage.BN) BanglaStrings else EnglishStrings

            AsifArafTheme(darkTheme = prefs.isDarkTheme) {
                CompositionLocalProvider(LocalAppStrings provides strings) {
                    AsifArafNavHost(viewModel = mainViewModel)
                }
            }
        }
    }
}
