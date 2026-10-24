package com.enigmacast.companion

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enigmacast.companion.data.LocalStore
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.CompanionApp

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val tag = LocalStore(newBase).language()
        if (tag.isBlank()) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val viewModel: AppViewModel = viewModel()
            CompanionApp(viewModel)
        }
    }
}
