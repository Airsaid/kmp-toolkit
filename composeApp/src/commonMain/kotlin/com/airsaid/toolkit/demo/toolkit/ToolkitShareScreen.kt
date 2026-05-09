package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.ShareContent
import com.airsaid.toolkit.ShareExcludedActivity
import com.airsaid.toolkit.ShareOptions
import com.airsaid.toolkit.ShareResult
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_share_combination
import com.airsaid.toolkit.demo.resources.action_share_image
import com.airsaid.toolkit.demo.resources.action_share_text
import com.airsaid.toolkit.demo.resources.action_share_url
import com.airsaid.toolkit.demo.resources.app_logo_not_found
import com.airsaid.toolkit.demo.resources.result_format
import com.airsaid.toolkit.demo.resources.share_cancelled
import com.airsaid.toolkit.demo.resources.share_completed
import com.airsaid.toolkit.demo.resources.share_default_text
import com.airsaid.toolkit.demo.resources.share_failed
import com.airsaid.toolkit.demo.resources.share_presented
import com.airsaid.toolkit.demo.resources.share_text_label
import com.airsaid.toolkit.demo.resources.share_title
import com.airsaid.toolkit.demo.resources.share_url_label
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitShareScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.ShareRoute } }
  val shareToolkit = remember { Toolkit.share() }
  val coroutineScope = rememberCoroutineScope()
  val appLogoBytes = rememberAppLogoBytes()
  val defaultText = stringResource(Res.string.share_default_text)
  val shareTitle = stringResource(Res.string.share_title)
  var text by remember(defaultText) { mutableStateOf(defaultText) }
  var url by remember { mutableStateOf("https://example.com") }
  var lastResult by remember { mutableStateOf<ShareDisplayResult?>(null) }

  ToolkitDemoPage(
    descriptionRes = item.descriptionRes,
    codeRes = item.codeRes,
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = text,
      onValueChange = { text = it },
      label = { Text(text = stringResource(Res.string.share_text_label)) },
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = url,
      onValueChange = { url = it },
      label = { Text(text = stringResource(Res.string.share_url_label)) },
      modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        coroutineScope.launch {
          lastResult = ShareDisplayResult.Result(
            shareToolkit.shareText(
              text = text,
              options = ShareOptions(title = shareTitle),
            ),
          )
        }
      }) {
        Text(text = stringResource(Res.string.action_share_text))
      }
      OutlinedButton(onClick = {
        coroutineScope.launch {
          lastResult = ShareDisplayResult.Result(
            shareToolkit.shareUrl(
              url = url,
              options = ShareOptions(title = shareTitle),
            ),
          )
        }
      }) {
        Text(text = stringResource(Res.string.action_share_url))
      }
      OutlinedButton(onClick = {
        val bytes = appLogoBytes
        if (bytes == null) {
          lastResult = ShareDisplayResult.LogoMissing
          return@OutlinedButton
        }
        coroutineScope.launch {
          lastResult = ShareDisplayResult.Result(
            shareToolkit.shareImage(
              bytes = bytes,
              mimeType = "image/png",
              options = ShareOptions(
                title = shareTitle,
                excludedActivities = listOf(ShareExcludedActivity.COPY_TO_PASTEBOARD),
              ),
            ),
          )
        }
      }) {
        Text(text = stringResource(Res.string.action_share_image))
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
        coroutineScope.launch {
          lastResult = ShareDisplayResult.Result(
            shareToolkit.share(
              contents = contents,
              options = ShareOptions(title = shareTitle),
            ),
          )
        }
      }) {
        Text(text = stringResource(Res.string.action_share_combination))
      }
    }
    StatusText(
      value = stringResource(
        Res.string.result_format,
        lastResult?.displayText() ?: "-",
      ),
    )
  }
}

private sealed interface ShareDisplayResult {
  data class Result(
    val value: ShareResult,
  ) : ShareDisplayResult

  data object LogoMissing : ShareDisplayResult
}

@Composable
private fun ShareDisplayResult.displayText(): String {
  return when (this) {
    is ShareDisplayResult.Result -> value.displayText()
    ShareDisplayResult.LogoMissing -> stringResource(Res.string.app_logo_not_found)
  }
}

@Composable
private fun ShareResult.displayText(): String {
  return when (this) {
    ShareResult.Presented -> stringResource(Res.string.share_presented)
    ShareResult.Completed -> stringResource(Res.string.share_completed)
    ShareResult.Cancelled -> stringResource(Res.string.share_cancelled)
    is ShareResult.Failed -> stringResource(Res.string.share_failed)
  }
}
