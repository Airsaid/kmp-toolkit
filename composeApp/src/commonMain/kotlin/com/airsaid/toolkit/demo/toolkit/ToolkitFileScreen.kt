package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.airsaid.toolkit.DirectoryPickerOptions
import com.airsaid.toolkit.FilePickerMode
import com.airsaid.toolkit.FilePickerOptions
import com.airsaid.toolkit.FileSaveOptions
import com.airsaid.toolkit.PlatformFile
import com.airsaid.toolkit.Toolkit
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitFileScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.FileRoute } }
  val fileToolkit = remember { Toolkit.fileToolkit() }
  val scope = rememberCoroutineScope()
  var lastFile by remember { mutableStateOf<PlatformFile?>(null) }
  var lastFiles by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }
  var lastMessage by remember { mutableStateOf("未执行") }
  var lastInfo by remember { mutableStateOf<String?>(null) }
  var lastError by remember { mutableStateOf<String?>(null) }
  val infoScrollState = rememberScrollState()

  fun launchSafely(block: suspend () -> Unit) {
    scope.launch {
      lastError = null
      try {
        block()
      } catch (error: Exception) {
        lastError = error.message ?: "未知错误"
      }
    }
  }

  fun updateSingleSelection(
    file: PlatformFile?,
    selectedMessage: String,
    emptyMessage: String,
  ) {
    lastFile = file
    lastFiles = emptyList()
    lastMessage = file?.let { "$selectedMessage: ${it.name}" } ?: emptyMessage
  }

  fun updateMultipleSelection(files: List<PlatformFile>) {
    lastFiles = files
    lastFile = files.firstOrNull()
    lastMessage = if (files.isEmpty()) {
      "未选择文件"
    } else {
      "已选择 ${files.size} 个文件"
    }
  }

  LaunchedEffect(lastFile) {
    val file = lastFile
    if (file == null) {
      lastInfo = null
      return@LaunchedEffect
    }
    lastInfo = try {
      file.withScopedAccess {
        buildFileInfo(it)
      }
    } catch (error: Exception) {
      "读取文件信息失败: ${error.message}"
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
        launchSafely {
          val picked = fileToolkit.pickFile(
            FilePickerOptions(title = "选择文件"),
          )
          updateSingleSelection(
            file = picked,
            selectedMessage = "已选择文件",
            emptyMessage = "未选择文件",
          )
        }
      }) {
        Text(text = "选择文件")
      }
      Button(onClick = {
        launchSafely {
          val picked = fileToolkit.pickFiles(
            FilePickerOptions(
              title = "多选文件",
              mode = FilePickerMode.Multiple(maxItems = 3),
            )
          )
          updateMultipleSelection(picked)
        }
      }) {
        Text(text = "多选文件")
      }
      Button(onClick = {
        launchSafely {
          val directory = fileToolkit.pickDirectory(
            DirectoryPickerOptions(title = "选择目录"),
          )
          updateSingleSelection(
            file = directory,
            selectedMessage = "已选择目录",
            emptyMessage = "未选择目录",
          )
        }
      }) {
        Text(text = "选择目录")
      }
      Button(onClick = {
        launchSafely {
          val saved = fileToolkit.saveFile(
            FileSaveOptions(
              suggestedName = "document",
              extension = "txt",
            )
          )
          updateSingleSelection(
            file = saved,
            selectedMessage = "已保存文件",
            emptyMessage = "未保存文件",
          )
        }
      }) {
        Text(text = "保存文件")
      }
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 160.dp)
        .verticalScroll(infoScrollState),
    ) {
      StatusText(value = "结果: $lastMessage")
      StatusText(value = lastInfo?.let { "文件信息: $it" } ?: "文件信息: -")
      ErrorText(message = lastError)
      if (lastFiles.isNotEmpty()) {
        StatusText(
          value = "多选结果: ${lastFiles.joinToString { it.name }}",
        )
      }
    }
  }
}

private suspend fun buildFileInfo(file: PlatformFile): String {
  val size = file.size()
  val mimeType = file.mimeType() ?: "-"
  val exists = file.exists()
  val isDirectory = file.isDirectory()
  val path = file.path ?: "-"
  return buildString {
    append("名称: ")
    append(file.name)
    append(", 大小: ")
    append(size)
    append(", MIME 类型: ")
    append(mimeType)
    append(", 存在: ")
    append(exists)
    append(", 目录: ")
    append(isDirectory)
    append(", 路径: ")
    append(path)
  }
}
