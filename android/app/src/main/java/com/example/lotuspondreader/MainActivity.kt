package com.example.lotuspondreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.lotuspondreader.data.SettingsRepository
import com.example.lotuspondreader.models.UserSettings
import com.example.lotuspondreader.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val settingsRepository = SettingsRepository(this)

    setContent {
      val userSettings by settingsRepository.userSettingsFlow.collectAsState(initial = UserSettings())
      val isDarkTheme = when (userSettings.themePreference) {
          "dark" -> true
          "light" -> false
          else -> isSystemInDarkTheme()
      }
      
      LaunchedEffect(isDarkTheme) {
          enableEdgeToEdge(
              statusBarStyle = if (isDarkTheme) {
                  SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
              } else {
                  SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
              },
              navigationBarStyle = if (isDarkTheme) {
                  SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
              } else {
                  SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
              }
          )
      }

      MyApplicationTheme(
          darkTheme = isDarkTheme,
          dynamicColor = userSettings.useDynamicColor
      ) { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
          MainNavigation() 
        } 
      }
    }
  }
}
