package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
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
import com.airsaid.toolkit.PlatformType
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_read_platform_type
import com.airsaid.toolkit.demo.resources.platform_current_format
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitPlatformScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.PlatformRoute } }
  var platformType by remember { mutableStateOf<PlatformType?>(null) }

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
        platformType = Toolkit.platformType()
      }) {
        Text(text = stringResource(Res.string.action_read_platform_type))
      }
    }
    StatusText(value = platformType?.let { type ->
      stringResource(Res.string.platform_current_format, type.displayName())
    })
  }
}

private fun PlatformType.displayName(): String {
  return when (this) {
    PlatformType.ANDROID -> "Android"
    PlatformType.IOS -> "iOS"
  }
}
