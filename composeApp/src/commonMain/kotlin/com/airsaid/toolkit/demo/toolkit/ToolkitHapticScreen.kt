package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.haptic_long_press
import com.airsaid.toolkit.demo.resources.haptic_text_handle_move
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitHapticScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.HapticRoute } }
  val haptics = LocalHapticFeedback.current

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
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      }) {
        Text(text = stringResource(Res.string.haptic_long_press))
      }
      OutlinedButton(onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }) {
        Text(text = stringResource(Res.string.haptic_text_handle_move))
      }
    }
  }
}
