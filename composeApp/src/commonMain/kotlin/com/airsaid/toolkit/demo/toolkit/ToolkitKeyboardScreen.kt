package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.airsaid.toolkit.KeyboardStatus
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.keyboard_input_label
import com.airsaid.toolkit.demo.resources.keyboard_status_format
import org.jetbrains.compose.resources.stringResource

@Composable
fun ToolkitKeyboardScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.KeyboardRoute } }
  val monitor = remember { Toolkit.keyboard() }
  var latestStatus by remember { mutableStateOf<KeyboardStatus?>(null) }
  var inputText by remember { mutableStateOf("") }

  LaunchedEffect(monitor) {
    monitor.observeKeyboardStatus().collect { status ->
      latestStatus = status
    }
  }

  ToolkitDemoPage(
    descriptionRes = item.descriptionRes,
    codeRes = item.codeRes,
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = inputText,
      onValueChange = { inputText = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text(text = stringResource(Res.string.keyboard_input_label)) },
    )
    StatusText(value = latestStatus?.let { status ->
      stringResource(Res.string.keyboard_status_format, status.isVisible, status.heightPx)
    })
  }
}
