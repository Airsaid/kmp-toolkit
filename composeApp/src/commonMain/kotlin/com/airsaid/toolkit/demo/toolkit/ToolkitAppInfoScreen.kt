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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitAppInfoScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.AppInfoRoute } }
  var appInfo by remember { mutableStateOf<AppInfo?>(null) }

  ToolkitDemoPage(
    description = item.description,
    code = item.code,
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
        Text(text = "读取应用信息")
      }
      Button(onClick = {
        appInfo = null
      }) {
        Text(text = "清空")
      }
    }
    StatusText(value = appInfo?.let(::buildAppInfoText))
  }
}

private fun buildAppInfoText(info: AppInfo): String {
  val rows = listOf(
    "包名" to info.packageName,
    "App 名称" to info.appName,
    "版本名" to info.versionName,
    "构建号" to info.buildNumber,
    "buildType" to info.buildType,
    "buildTime" to info.buildTime,
  )
  return rows.joinToString("\n") { (label, value) ->
    "$label: ${value.ifBlank { "-" }}"
  }
}
