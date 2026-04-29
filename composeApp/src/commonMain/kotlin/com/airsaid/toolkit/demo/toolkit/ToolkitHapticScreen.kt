package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.HapticFeedbackType
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.haptic_error
import com.airsaid.toolkit.demo.resources.haptic_selection
import com.airsaid.toolkit.demo.resources.haptic_success
import com.airsaid.toolkit.demo.resources.haptic_warning
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitHapticScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.HapticRoute } }
  val haptics = remember { Toolkit.hapticFeedback() }

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
        haptics.perform(HapticFeedbackType.SELECTION)
      }) {
        Text(text = stringResource(Res.string.haptic_selection))
      }
      OutlinedButton(onClick = {
        haptics.perform(HapticFeedbackType.SUCCESS)
      }) {
        Text(text = stringResource(Res.string.haptic_success))
      }
      OutlinedButton(onClick = {
        haptics.perform(HapticFeedbackType.WARNING)
      }) {
        Text(text = stringResource(Res.string.haptic_warning))
      }
      OutlinedButton(onClick = {
        haptics.perform(HapticFeedbackType.ERROR)
      }) {
        Text(text = stringResource(Res.string.haptic_error))
      }
    }
  }
}
