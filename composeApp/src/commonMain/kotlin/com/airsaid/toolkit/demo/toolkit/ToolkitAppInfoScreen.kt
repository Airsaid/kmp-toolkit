package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.AppInfo
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_clear
import com.airsaid.toolkit.demo.resources.action_read_app_info
import com.airsaid.toolkit.demo.resources.app_info_app_name
import com.airsaid.toolkit.demo.resources.app_info_build_number
import com.airsaid.toolkit.demo.resources.app_info_package_name
import com.airsaid.toolkit.demo.resources.app_info_version_name
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitAppInfoScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.AppInfoRoute } }
  var appInfo by remember { mutableStateOf<AppInfo?>(null) }

  ToolkitDemoPage(
    descriptionRes = item.descriptionRes,
    codeRes = item.codeRes,
    modifier = modifier,
  ) {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        appInfo = Toolkit.appInfo()
      }) {
        Text(text = stringResource(Res.string.action_read_app_info))
      }
      Button(onClick = {
        appInfo = null
      }) {
        Text(text = stringResource(Res.string.action_clear))
      }
    }
    StatusText(value = appInfo?.let { info ->
      buildAppInfoText(
        info = info,
        packageNameLabel = stringResource(Res.string.app_info_package_name),
        appNameLabel = stringResource(Res.string.app_info_app_name),
        versionNameLabel = stringResource(Res.string.app_info_version_name),
        buildNumberLabel = stringResource(Res.string.app_info_build_number),
      )
    })
  }
}

private fun buildAppInfoText(
  info: AppInfo,
  packageNameLabel: String,
  appNameLabel: String,
  versionNameLabel: String,
  buildNumberLabel: String,
): String {
  val rows = listOf(
    packageNameLabel to info.packageName,
    appNameLabel to info.appName,
    versionNameLabel to info.versionName,
    buildNumberLabel to info.buildNumber,
  )
  return rows.joinToString("\n") { (label, value) ->
    "$label: ${formatAppInfoValue(value)}"
  }
}

private fun formatAppInfoValue(value: String?): String {
  return value?.takeUnless { it.isBlank() } ?: "-"
}
