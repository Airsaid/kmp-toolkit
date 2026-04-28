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
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitClipboardScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.ClipboardRoute } }
  val scope = rememberCoroutineScope()
  val clipboard = remember { Toolkit.clipboard() }
  val appLogoBytes = rememberAppLogoBytes()
  var inputText by remember { mutableStateOf("Toolkit Demo") }
  var latestText by remember { mutableStateOf<String?>(null) }
  var latestSnapshot by remember { mutableStateOf<ClipboardSnapshot?>(null) }
  var latestImageInfo by remember { mutableStateOf<String?>(null) }
  var isObserving by remember { mutableStateOf(false) }

  LaunchedEffect(isObserving) {
    if (!isObserving) return@LaunchedEffect
    clipboard.observeClipboard().collect { snapshot ->
      latestSnapshot = snapshot
      latestText = snapshot.firstTextOrNull()
      latestImageInfo = snapshot.imageInfo()
    }
  }

  ToolkitDemoPage(
    description = item.description,
    code = item.code,
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = inputText,
      onValueChange = { inputText = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text(text = "写入内容") },
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        clipboard.setText(inputText)
      }) {
        Text(text = "写入")
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
        Text(text = "写入富文本与 URI")
      }
      OutlinedButton(onClick = {
        val bytes = appLogoBytes
        if (bytes == null) {
          latestImageInfo = "应用 Logo 未找到"
          return@OutlinedButton
        }
        clipboard.setContents(
          listOf(ClipboardContent.Image(bytes, "image/png"))
        )
      }) {
        Text(text = "写入图片")
      }
      OutlinedButton(onClick = {
        scope.launch {
          val snapshot = clipboard.getSnapshot()
          latestSnapshot = snapshot
          latestText = snapshot.firstTextOrNull()
          latestImageInfo = snapshot.imageInfo()
        }
      }) {
        Text(text = "读取")
      }
      OutlinedButton(onClick = {
        scope.launch {
          val snapshot = clipboard.getSnapshot()
          latestSnapshot = snapshot
          latestImageInfo = snapshot.imageInfo()
        }
      }) {
        Text(text = "读取图片")
      }
      OutlinedButton(onClick = {
        clipboard.clear()
      }) {
        Text(text = "清空")
      }
      OutlinedButton(onClick = {
        isObserving = true
      }) {
        Text(text = "开始监听")
      }
      OutlinedButton(onClick = { isObserving = false }) {
        Text(text = "停止监听")
      }
  }
  StatusText(value = latestText?.let { "剪贴板文本: $it" } ?: "剪贴板文本: -")
  StatusText(value = latestImageInfo?.let { "剪贴板图片: $it" } ?: "剪贴板图片: -")
  StatusText(value = latestSnapshot?.let { "剪贴板内容: ${it.describe()}" } ?: "剪贴板内容: -")
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

private fun ClipboardSnapshot.describe(): String {
  if (contents.isEmpty()) return "空"
  return contents.joinToString(separator = ", ") { content ->
    when (content) {
      is ClipboardContent.Text -> "文本"
      is ClipboardContent.RichText -> "富文本(${content.format})"
      is ClipboardContent.Uri -> "URI"
      is ClipboardContent.Image -> "图片"
    }
  }
}

private fun ClipboardSnapshot.firstImageOrNull(): ClipboardContent.Image? {
  return contents.firstNotNullOfOrNull { content ->
    content as? ClipboardContent.Image
  }
}

private fun ClipboardSnapshot.imageInfo(): String {
  val image = firstImageOrNull() ?: return "未找到图片"
  val type = image.mimeType ?: "-"
  return "图片字节: ${image.bytes.size}, 类型: $type"
}
