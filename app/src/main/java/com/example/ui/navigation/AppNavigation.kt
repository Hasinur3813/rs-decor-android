package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.Design
import com.example.data.repository.CatalogRepository
import com.example.ui.details.DesignDetailsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.theme.DarkObsidian
import com.example.ui.viewmodel.CatalogViewModel

sealed interface Screen {
    data object Home : Screen
    data class Details(val design: Design) : Screen
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val viewModel: CatalogViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = CatalogRepository.firestoreForDatabase(databaseId)
                CatalogViewModel(CatalogRepository(db), com.example.util.NetworkMonitor(app))
            }
        }
    )

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .statusBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                is Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onDesignClick = { design ->
                            currentScreen = Screen.Details(design)
                        }
                    )
                }
                is Screen.Details -> {
                    DesignDetailsScreen(
                        design = screen.design,
                        appSettings = uiState.appSettings,
                        onBackClick = { currentScreen = Screen.Home }
                    )
                }
            }
        }
    }
}
