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
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_app_details
import com.airsaid.toolkit.demo.resources.action_app_store_details
import com.airsaid.toolkit.demo.resources.action_dial
import com.airsaid.toolkit.demo.resources.action_notification_settings
import com.airsaid.toolkit.demo.resources.action_open_url
import com.airsaid.toolkit.demo.resources.action_send_email
import com.airsaid.toolkit.demo.resources.action_sms
import com.airsaid.toolkit.demo.resources.action_system_settings
import com.airsaid.toolkit.demo.resources.email_body_describe_issue
import com.airsaid.toolkit.demo.resources.email_subject_feedback
import com.airsaid.toolkit.demo.resources.sms_body_hello
import com.airsaid.toolkit.isIos
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitAppNavigatorScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.AppNavigatorRoute } }
  val navigator = remember { Toolkit.navigator() }
  val appStoreId = remember {
    if (Toolkit.platform.isIos) {
      "123456789"
    } else {
      "com.example.app"
    }
  }
  val emailSubject = stringResource(Res.string.email_subject_feedback)
  val emailBody = stringResource(Res.string.email_body_describe_issue)
  val smsBody = stringResource(Res.string.sms_body_hello)
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
        navigator.navigateToSystemSettings()
      }) {
        Text(text = stringResource(Res.string.action_system_settings))
      }
      OutlinedButton(onClick = {
        navigator.navigateToAppDetails()
      }) {
        Text(text = stringResource(Res.string.action_app_details))
      }
      OutlinedButton(onClick = {
        navigator.navigateToNotificationSettings()
      }) {
        Text(text = stringResource(Res.string.action_notification_settings))
      }
      OutlinedButton(onClick = {
        navigator.navigateToEmail(
          to = "support@example.com",
          subject = emailSubject,
          body = emailBody,
        )
      }) {
        Text(text = stringResource(Res.string.action_send_email))
      }
      OutlinedButton(onClick = {
        navigator.navigateToDial("10086")
      }) {
        Text(text = stringResource(Res.string.action_dial))
      }
      OutlinedButton(onClick = {
        navigator.navigateToSms(
          phone = "10086",
          body = smsBody,
        )
      }) {
        Text(text = stringResource(Res.string.action_sms))
      }
      OutlinedButton(onClick = {
        navigator.navigateToAppStoreDetails(appStoreId)
      }) {
        Text(text = stringResource(Res.string.action_app_store_details))
      }
      OutlinedButton(onClick = {
        navigator.navigateToUrl("https://example.com")
      }) {
        Text(text = stringResource(Res.string.action_open_url))
      }
    }
  }
}
