package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.airsaid.toolkit.PlatformType
import com.airsaid.toolkit.Toolkit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitAppNavigatorScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.AppNavigatorRoute } }
  val navigator = remember { Toolkit.appNavigator() }
  val appStoreId = remember {
    if (Toolkit.platformType() == PlatformType.IOS) {
      "123456789"
    } else {
      "com.example.app"
    }
  }
  ToolkitDemoPage(
    description = item.description,
    code = item.code,
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
        Text(text = "系统设置")
      }
      OutlinedButton(onClick = {
        navigator.navigateToAppDetails()
      }) {
        Text(text = "应用详情")
      }
      OutlinedButton(onClick = {
        navigator.navigateToNotificationSettings()
      }) {
        Text(text = "通知设置")
      }
      OutlinedButton(onClick = {
        navigator.navigateToEmail(
          to = "support@example.com",
          subject = "反馈",
          body = "请描述你的问题",
        )
      }) {
        Text(text = "发送邮件")
      }
      OutlinedButton(onClick = {
        navigator.navigateToDial("10086")
      }) {
        Text(text = "拨号")
      }
      OutlinedButton(onClick = {
        navigator.navigateToSms(
          phone = "10086",
          body = "你好",
        )
      }) {
        Text(text = "短信")
      }
      OutlinedButton(onClick = {
        navigator.navigateToAppStoreDetails(appStoreId)
      }) {
        Text(text = "应用商店详情")
      }
      OutlinedButton(onClick = {
        navigator.navigateToUrl("https://example.com")
      }) {
        Text(text = "打开 URL")
      }
    }
  }
}
