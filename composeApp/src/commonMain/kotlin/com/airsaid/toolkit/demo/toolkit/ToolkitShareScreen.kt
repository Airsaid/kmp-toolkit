package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.ShareContent
import com.airsaid.toolkit.ShareExcludedActivity
import com.airsaid.toolkit.ShareOptions
import com.airsaid.toolkit.Toolkit
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitShareScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.ShareRoute } }
  val shareToolkit = remember { Toolkit.shareToolkit() }
  val appLogoBytes = rememberAppLogoBytes()
  var text by remember { mutableStateOf("分享内容") }
  var url by remember { mutableStateOf("https://example.com") }
  var lastResult by remember { mutableStateOf<String?>(null) }

  ToolkitDemoPage(
    description = item.description,
    code = item.code,
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = text,
      onValueChange = { text = it },
      label = { Text(text = "文本") },
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = url,
      onValueChange = { url = it },
      label = { Text(text = "链接") },
      modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        val success = shareToolkit.shareText(
          text = text,
          options = ShareOptions(title = "分享到"),
        )
        lastResult = if (success) "分享文本成功" else "分享文本失败"
      }) {
        Text(text = "分享文本")
      }
      OutlinedButton(onClick = {
        val success = shareToolkit.shareUrl(
          url = url,
          options = ShareOptions(title = "分享到"),
        )
        lastResult = if (success) "分享链接成功" else "分享链接失败"
      }) {
        Text(text = "分享链接")
      }
      OutlinedButton(onClick = {
        val bytes = appLogoBytes
        if (bytes == null) {
          lastResult = "应用 Logo 未找到"
          return@OutlinedButton
        }
        val success = shareToolkit.shareImage(
          bytes = bytes,
          mimeType = "image/png",
          options = ShareOptions(
            title = "分享到",
            excludedActivities = listOf(ShareExcludedActivity.COPY_TO_PASTEBOARD),
          ),
        )
        lastResult = if (success) "分享图片成功" else "分享图片失败"
      }) {
        Text(text = "分享图片")
      }
      OutlinedButton(onClick = {
        val contents = buildList {
          if (text.isNotBlank()) {
            add(ShareContent.Text(text))
          }
          if (url.isNotBlank()) {
            add(ShareContent.Url(url))
          }
          appLogoBytes?.let { add(ShareContent.Image(it, "image/png")) }
        }
        val success = shareToolkit.share(
          contents = contents,
          options = ShareOptions(title = "分享到"),
        )
        lastResult = if (success) "分享组合成功" else "分享组合失败"
      }) {
        Text(text = "分享组合")
      }
    }
    StatusText(value = lastResult?.let { "结果: $it" } ?: "结果: -")
  }
}
