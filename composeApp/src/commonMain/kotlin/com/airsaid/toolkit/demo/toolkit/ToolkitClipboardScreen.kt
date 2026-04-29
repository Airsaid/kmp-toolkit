package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.ClipboardContent
import com.airsaid.toolkit.ClipboardSnapshot
import com.airsaid.toolkit.RichTextFormat
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_clear
import com.airsaid.toolkit.demo.resources.action_read
import com.airsaid.toolkit.demo.resources.action_read_image
import com.airsaid.toolkit.demo.resources.action_start_monitoring
import com.airsaid.toolkit.demo.resources.action_stop_monitoring
import com.airsaid.toolkit.demo.resources.action_write
import com.airsaid.toolkit.demo.resources.action_write_image
import com.airsaid.toolkit.demo.resources.action_write_rich_text_uri
import com.airsaid.toolkit.demo.resources.app_logo_not_found
import com.airsaid.toolkit.demo.resources.clipboard_default_text
import com.airsaid.toolkit.demo.resources.clipboard_content_empty
import com.airsaid.toolkit.demo.resources.clipboard_content_image
import com.airsaid.toolkit.demo.resources.clipboard_content_rich_text
import com.airsaid.toolkit.demo.resources.clipboard_content_text
import com.airsaid.toolkit.demo.resources.clipboard_content_uri
import com.airsaid.toolkit.demo.resources.clipboard_contents_status
import com.airsaid.toolkit.demo.resources.clipboard_image_status
import com.airsaid.toolkit.demo.resources.clipboard_input_label
import com.airsaid.toolkit.demo.resources.clipboard_text_status
import com.airsaid.toolkit.demo.resources.image_info_format
import com.airsaid.toolkit.demo.resources.image_not_found
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitClipboardScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.ClipboardRoute } }
  val scope = rememberCoroutineScope()
  val clipboard = remember { Toolkit.clipboard() }
  val appLogoBytes = rememberAppLogoBytes()
  val defaultInputText = stringResource(Res.string.clipboard_default_text)
  var inputText by remember(defaultInputText) { mutableStateOf(defaultInputText) }
  var latestText by remember { mutableStateOf<String?>(null) }
  var latestSnapshot by remember { mutableStateOf<ClipboardSnapshot?>(null) }
  var latestImageMissingLogo by remember { mutableStateOf(false) }
  var isObserving by remember { mutableStateOf(false) }

  LaunchedEffect(isObserving) {
    if (!isObserving) return@LaunchedEffect
    clipboard.observeClipboard().collect { snapshot ->
      latestSnapshot = snapshot
      latestText = snapshot.firstTextOrNull()
      latestImageMissingLogo = false
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
      label = { Text(text = stringResource(Res.string.clipboard_input_label)) },
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        clipboard.setText(inputText)
      }) {
        Text(text = stringResource(Res.string.action_write))
      }
      OutlinedButton(onClick = {
        clipboard.setContents(
          listOf(
            ClipboardContent.RichText(
              text = "<b>${inputText}</b>",
              format = RichTextFormat.HTML,
              plainText = inputText,
            ),
            ClipboardContent.Uri("https://example.com"),
          )
        )
      }) {
        Text(text = stringResource(Res.string.action_write_rich_text_uri))
      }
      OutlinedButton(onClick = {
        val bytes = appLogoBytes
        if (bytes == null) {
          latestImageMissingLogo = true
          return@OutlinedButton
        }
        latestImageMissingLogo = false
        clipboard.setContents(
          listOf(ClipboardContent.Image(bytes, "image/png"))
        )
      }) {
        Text(text = stringResource(Res.string.action_write_image))
      }
      OutlinedButton(onClick = {
        scope.launch {
          val snapshot = clipboard.getSnapshot()
          latestSnapshot = snapshot
          latestText = snapshot.firstTextOrNull()
          latestImageMissingLogo = false
        }
      }) {
        Text(text = stringResource(Res.string.action_read))
      }
      OutlinedButton(onClick = {
        scope.launch {
          val snapshot = clipboard.getSnapshot()
          latestSnapshot = snapshot
          latestImageMissingLogo = false
        }
      }) {
        Text(text = stringResource(Res.string.action_read_image))
      }
      OutlinedButton(onClick = {
        clipboard.clear()
      }) {
        Text(text = stringResource(Res.string.action_clear))
      }
      OutlinedButton(onClick = {
        isObserving = true
      }) {
        Text(text = stringResource(Res.string.action_start_monitoring))
      }
      OutlinedButton(onClick = { isObserving = false }) {
        Text(text = stringResource(Res.string.action_stop_monitoring))
      }
  }
  StatusText(value = stringResource(Res.string.clipboard_text_status, latestText ?: "-"))
  StatusText(
    value = stringResource(
      Res.string.clipboard_image_status,
      when {
        latestImageMissingLogo -> stringResource(Res.string.app_logo_not_found)
        latestSnapshot != null -> latestSnapshot.imageInfo()
        else -> "-"
      },
    ),
  )
  StatusText(
    value = stringResource(
      Res.string.clipboard_contents_status,
      latestSnapshot?.describe() ?: "-",
    ),
  )
}
}

private fun ClipboardSnapshot.firstTextOrNull(): String? {
  return contents.firstNotNullOfOrNull { content ->
    when (content) {
      is ClipboardContent.Text -> content.text
      is ClipboardContent.RichText -> content.plainText ?: content.text
      else -> null
    }
  }
}

@Composable
private fun ClipboardSnapshot.describe(): String {
  if (contents.isEmpty()) return stringResource(Res.string.clipboard_content_empty)
  val labels = mutableListOf<String>()
  for (content in contents) {
    labels += when (content) {
      is ClipboardContent.Text -> stringResource(Res.string.clipboard_content_text)
      is ClipboardContent.RichText -> stringResource(
        Res.string.clipboard_content_rich_text,
        content.format,
      )
      is ClipboardContent.Uri -> stringResource(Res.string.clipboard_content_uri)
      is ClipboardContent.Image -> stringResource(Res.string.clipboard_content_image)
    }
  }
  return labels.joinToString(separator = ", ")
}

private fun ClipboardSnapshot.firstImageOrNull(): ClipboardContent.Image? {
  return contents.firstNotNullOfOrNull { content ->
    content as? ClipboardContent.Image
  }
}

@Composable
private fun ClipboardSnapshot?.imageInfo(): String {
  val image = this?.firstImageOrNull() ?: return stringResource(Res.string.image_not_found)
  val type = image.mimeType ?: "-"
  return stringResource(Res.string.image_info_format, image.bytes.size, type)
}
