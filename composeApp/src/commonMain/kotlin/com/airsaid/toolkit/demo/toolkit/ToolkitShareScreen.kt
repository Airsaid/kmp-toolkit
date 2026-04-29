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
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_share_combination
import com.airsaid.toolkit.demo.resources.action_share_image
import com.airsaid.toolkit.demo.resources.action_share_text
import com.airsaid.toolkit.demo.resources.action_share_url
import com.airsaid.toolkit.demo.resources.app_logo_not_found
import com.airsaid.toolkit.demo.resources.result_format
import com.airsaid.toolkit.demo.resources.share_combination_failure
import com.airsaid.toolkit.demo.resources.share_combination_success
import com.airsaid.toolkit.demo.resources.share_default_text
import com.airsaid.toolkit.demo.resources.share_image_failure
import com.airsaid.toolkit.demo.resources.share_image_success
import com.airsaid.toolkit.demo.resources.share_text_failure
import com.airsaid.toolkit.demo.resources.share_text_label
import com.airsaid.toolkit.demo.resources.share_text_success
import com.airsaid.toolkit.demo.resources.share_title
import com.airsaid.toolkit.demo.resources.share_url_failure
import com.airsaid.toolkit.demo.resources.share_url_label
import com.airsaid.toolkit.demo.resources.share_url_success
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitShareScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.ShareRoute } }
  val shareToolkit = remember { Toolkit.shareToolkit() }
  val appLogoBytes = rememberAppLogoBytes()
  val defaultText = stringResource(Res.string.share_default_text)
  val shareTitle = stringResource(Res.string.share_title)
  var text by remember(defaultText) { mutableStateOf(defaultText) }
  var url by remember { mutableStateOf("https://example.com") }
  var lastResult by remember { mutableStateOf<ShareResult?>(null) }

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
        val success = shareToolkit.shareText(
          text = text,
          options = ShareOptions(title = shareTitle),
        )
        lastResult = if (success) ShareResult.TextSuccess else ShareResult.TextFailure
      }) {
        Text(text = stringResource(Res.string.action_share_text))
      }
      OutlinedButton(onClick = {
        val success = shareToolkit.shareUrl(
          url = url,
          options = ShareOptions(title = shareTitle),
        )
        lastResult = if (success) ShareResult.UrlSuccess else ShareResult.UrlFailure
      }) {
        Text(text = stringResource(Res.string.action_share_url))
      }
      OutlinedButton(onClick = {
        val bytes = appLogoBytes
        if (bytes == null) {
          lastResult = ShareResult.LogoMissing
          return@OutlinedButton
        }
        val success = shareToolkit.shareImage(
          bytes = bytes,
          mimeType = "image/png",
          options = ShareOptions(
            title = shareTitle,
            excludedActivities = listOf(ShareExcludedActivity.COPY_TO_PASTEBOARD),
          ),
        )
        lastResult = if (success) ShareResult.ImageSuccess else ShareResult.ImageFailure
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
        val success = shareToolkit.share(
          contents = contents,
          options = ShareOptions(title = shareTitle),
        )
        lastResult = if (success) ShareResult.CombinationSuccess else ShareResult.CombinationFailure
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

private enum class ShareResult {
  TextSuccess,
  TextFailure,
  UrlSuccess,
  UrlFailure,
  ImageSuccess,
  ImageFailure,
  CombinationSuccess,
  CombinationFailure,
  LogoMissing,
}

@Composable
private fun ShareResult.displayText(): String {
  return when (this) {
    ShareResult.TextSuccess -> stringResource(Res.string.share_text_success)
    ShareResult.TextFailure -> stringResource(Res.string.share_text_failure)
    ShareResult.UrlSuccess -> stringResource(Res.string.share_url_success)
    ShareResult.UrlFailure -> stringResource(Res.string.share_url_failure)
    ShareResult.ImageSuccess -> stringResource(Res.string.share_image_success)
    ShareResult.ImageFailure -> stringResource(Res.string.share_image_failure)
    ShareResult.CombinationSuccess -> stringResource(Res.string.share_combination_success)
    ShareResult.CombinationFailure -> stringResource(Res.string.share_combination_failure)
    ShareResult.LogoMissing -> stringResource(Res.string.app_logo_not_found)
  }
}
