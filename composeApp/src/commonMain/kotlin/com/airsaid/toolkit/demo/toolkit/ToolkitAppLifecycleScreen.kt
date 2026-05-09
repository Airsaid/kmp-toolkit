package com.airsaid.toolkit.demo.toolkit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.airsaid.toolkit.AppLifecycleStatus
import com.airsaid.toolkit.AppStartType
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.app_lifecycle_event_format
import com.airsaid.toolkit.demo.resources.app_lifecycle_status_format
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ToolkitAppLifecycleScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.AppLifecycleRoute } }
  val monitor = remember { Toolkit.lifecycle() }
  var latestStatus by remember { mutableStateOf<AppLifecycleStatus?>(null) }
  var latestStartType by remember { mutableStateOf<AppStartType?>(null) }

  LaunchedEffect(monitor) {
    launch {
      monitor.observeAppStartEvents().collect { startType ->
        latestStartType = startType
      }
    }
    launch {
      monitor.observeAppLifecycle().collect { status ->
        latestStatus = status
      }
    }
  }

  ToolkitDemoPage(
    descriptionRes = item.descriptionRes,
    codeRes = item.codeRes,
    modifier = modifier,
  ) {
    StatusText(value = latestStatus?.let { status ->
      stringResource(
        Res.string.app_lifecycle_status_format,
        status.isInForeground,
        status.isVisible,
        status.lastStartType?.name ?: "-",
      )
    })
    StatusText(
      value = stringResource(
        Res.string.app_lifecycle_event_format,
        latestStartType?.name ?: "-",
      )
    )
  }
}
