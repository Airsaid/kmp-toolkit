@file:OptIn(ExperimentalMaterial3Api::class)

package com.airsaid.toolkit.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airsaid.toolkit.demo.toolkit.ToolkitDemoItems

@Composable
fun DemoApp() {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: DemoRoutes.HomeRoute
  val currentScreenName = resolveScreenTitle(currentRoute)
  val snackbarHostState = remember { SnackbarHostState() }

  MaterialTheme {
    Scaffold(
      topBar = {
        AppBar(
          currentScreenName = currentScreenName,
          canNavigateBack = navController.previousBackStackEntry != null,
          navigateUp = { navController.navigateUp() }
        )
      },
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
      DemoNavHost(navController, Modifier.fillMaxSize().padding(innerPadding))
    }
  }
}

private fun resolveScreenTitle(route: String): String {
  val toolkitTitle = ToolkitDemoItems.all.firstOrNull { it.route == route }?.title
  return toolkitTitle ?: route
}

@Composable
private fun AppBar(
  currentScreenName: String,
  canNavigateBack: Boolean,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TopAppBar(
    title = { Text(currentScreenName) },
    colors = TopAppBarDefaults.mediumTopAppBarColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer
    ),
    modifier = modifier,
    navigationIcon = {
      if (canNavigateBack) {
        IconButton(onClick = navigateUp) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null
          )
        }
      }
    }
  )
}
