package com.airsaid.toolkit.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.airsaid.toolkit.demo.toolkit.ToolkitAppInfoScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitAppLifecycleScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitAppNavigatorScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitClipboardScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitDeviceInfoScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitFileScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitHapticScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitHomeScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitKeyboardScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitNetworkScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitPlatformScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitSensorScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitShareScreen
import com.airsaid.toolkit.demo.toolkit.ToolkitDemoItems

object DemoRoutes {
  const val HomeRoute = "ToolkitHome"
}

@Composable
fun DemoNavHost(
  navController: NavHostController,
  modifier: Modifier = Modifier,
) {
  val fullSizeModifier = Modifier.fillMaxSize()
  val paddedModifier = fullSizeModifier.padding(16.dp)

  NavHost(
    navController = navController,
    startDestination = DemoRoutes.HomeRoute,
    modifier = modifier,
  ) {
    composable(route = DemoRoutes.HomeRoute) {
      ToolkitHomeScreen(
        modifier = paddedModifier,
      ) { route ->
        navController.navigate(route)
      }
    }

    composable(route = ToolkitDemoItems.AppLifecycleRoute) {
      ToolkitAppLifecycleScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.AppInfoRoute) {
      ToolkitAppInfoScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.ClipboardRoute) {
      ToolkitClipboardScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.HapticRoute) {
      ToolkitHapticScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.NetworkRoute) {
      ToolkitNetworkScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.SensorRoute) {
      ToolkitSensorScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.AppNavigatorRoute) {
      ToolkitAppNavigatorScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.FileRoute) {
      ToolkitFileScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.ShareRoute) {
      ToolkitShareScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.DeviceInfoRoute) {
      ToolkitDeviceInfoScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.PlatformRoute) {
      ToolkitPlatformScreen(modifier = fullSizeModifier)
    }

    composable(route = ToolkitDemoItems.KeyboardRoute) {
      ToolkitKeyboardScreen(modifier = fullSizeModifier)
    }

  }
}
