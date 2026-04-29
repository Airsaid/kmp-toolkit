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
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_pick_directory
import com.airsaid.toolkit.demo.resources.action_pick_file
import com.airsaid.toolkit.demo.resources.action_pick_files
import com.airsaid.toolkit.demo.resources.action_save_file
import com.airsaid.toolkit.demo.resources.file_info_format
import com.airsaid.toolkit.demo.resources.file_info_read_failed
import com.airsaid.toolkit.demo.resources.file_info_status
import com.airsaid.toolkit.demo.resources.file_message_with_name
import com.airsaid.toolkit.demo.resources.file_multi_select_result
import com.airsaid.toolkit.demo.resources.file_no_directory_selected
import com.airsaid.toolkit.demo.resources.file_no_file_selected
import com.airsaid.toolkit.demo.resources.file_not_run
import com.airsaid.toolkit.demo.resources.file_not_saved
import com.airsaid.toolkit.demo.resources.file_picker_title_directory
import com.airsaid.toolkit.demo.resources.file_picker_title_file
import com.airsaid.toolkit.demo.resources.file_picker_title_files
import com.airsaid.toolkit.demo.resources.file_saved_file
import com.airsaid.toolkit.demo.resources.file_selected_count
import com.airsaid.toolkit.demo.resources.file_selected_directory
import com.airsaid.toolkit.demo.resources.file_selected_file
import com.airsaid.toolkit.demo.resources.result_format
import com.airsaid.toolkit.demo.resources.unknown_error
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitFileScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.FileRoute } }
  val fileToolkit = remember { Toolkit.fileToolkit() }
  val scope = rememberCoroutineScope()
  var lastFile by remember { mutableStateOf<PlatformFile?>(null) }
  var lastFiles by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }
  var lastMessage by remember { mutableStateOf<FileMessage>(FileMessage.NotRun) }
  var lastInfo by remember { mutableStateOf<FileInfo?>(null) }
  var lastInfoError by remember { mutableStateOf<FileError?>(null) }
  var lastError by remember { mutableStateOf<FileError?>(null) }
  val infoScrollState = rememberScrollState()
  val pickFileTitle = stringResource(Res.string.file_picker_title_file)
  val pickFilesTitle = stringResource(Res.string.file_picker_title_files)
  val pickDirectoryTitle = stringResource(Res.string.file_picker_title_directory)

  fun launchSafely(block: suspend () -> Unit) {
    scope.launch {
      lastError = null
      try {
        block()
      } catch (error: Exception) {
        lastError = FileError(error.message)
      }
    }
  }

  fun updateSingleSelection(
    file: PlatformFile?,
    selectedMessage: StringResource,
    emptyMessage: StringResource,
  ) {
    lastFile = file
    lastFiles = emptyList()
    lastMessage = file?.let {
      FileMessage.Named(selectedMessage, it.name)
    } ?: FileMessage.Plain(emptyMessage)
  }

  fun updateMultipleSelection(files: List<PlatformFile>) {
    lastFiles = files
    lastFile = files.firstOrNull()
    lastMessage = if (files.isEmpty()) {
      FileMessage.Plain(Res.string.file_no_file_selected)
    } else {
      FileMessage.FileCount(files.size)
    }
  }

  LaunchedEffect(lastFile) {
    val file = lastFile
    if (file == null) {
      lastInfo = null
      lastInfoError = null
      return@LaunchedEffect
    }
    lastInfo = try {
      lastInfoError = null
      file.withScopedAccess {
        buildFileInfo(it)
      }
    } catch (error: Exception) {
      lastInfoError = FileError(error.message)
      null
    }
  }

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
        launchSafely {
          val picked = fileToolkit.pickFile(
            FilePickerOptions(title = pickFileTitle),
          )
          updateSingleSelection(
            file = picked,
            selectedMessage = Res.string.file_selected_file,
            emptyMessage = Res.string.file_no_file_selected,
          )
        }
      }) {
        Text(text = stringResource(Res.string.action_pick_file))
      }
      Button(onClick = {
        launchSafely {
          val picked = fileToolkit.pickFiles(
            FilePickerOptions(
              title = pickFilesTitle,
              mode = FilePickerMode.Multiple(maxItems = 3),
            )
          )
          updateMultipleSelection(picked)
        }
      }) {
        Text(text = stringResource(Res.string.action_pick_files))
      }
      Button(onClick = {
        launchSafely {
          val directory = fileToolkit.pickDirectory(
            DirectoryPickerOptions(title = pickDirectoryTitle),
          )
          updateSingleSelection(
            file = directory,
            selectedMessage = Res.string.file_selected_directory,
            emptyMessage = Res.string.file_no_directory_selected,
          )
        }
      }) {
        Text(text = stringResource(Res.string.action_pick_directory))
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
            selectedMessage = Res.string.file_saved_file,
            emptyMessage = Res.string.file_not_saved,
          )
        }
      }) {
        Text(text = stringResource(Res.string.action_save_file))
      }
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 160.dp)
        .verticalScroll(infoScrollState),
    ) {
      StatusText(value = stringResource(Res.string.result_format, lastMessage.displayText()))
      StatusText(
        value = stringResource(
          Res.string.file_info_status,
          lastInfo?.displayText()
            ?: lastInfoError?.let {
              stringResource(Res.string.file_info_read_failed, it.displayText())
            }
            ?: "-",
        ),
      )
      ErrorText(message = lastError?.displayText())
      if (lastFiles.isNotEmpty()) {
        StatusText(
          value = stringResource(
            Res.string.file_multi_select_result,
            lastFiles.joinToString { it.name },
          ),
        )
      }
    }
  }
}

private sealed interface FileMessage {
  data object NotRun : FileMessage
  data class Plain(val textRes: StringResource) : FileMessage
  data class Named(val labelRes: StringResource, val name: String) : FileMessage
  data class FileCount(val count: Int) : FileMessage
}

private data class FileInfo(
  val name: String,
  val size: Long,
  val mimeType: String,
  val exists: Boolean,
  val isDirectory: Boolean,
  val path: String,
)

private data class FileError(
  val message: String?,
)

@Composable
private fun FileMessage.displayText(): String {
  return when (this) {
    FileMessage.NotRun -> stringResource(Res.string.file_not_run)
    is FileMessage.Plain -> stringResource(textRes)
    is FileMessage.Named -> stringResource(
      Res.string.file_message_with_name,
      stringResource(labelRes),
      name,
    )
    is FileMessage.FileCount -> pluralStringResource(
      Res.plurals.file_selected_count,
      count,
      count,
    )
  }
}

@Composable
private fun FileInfo.displayText(): String {
  return stringResource(
    Res.string.file_info_format,
    name,
    size,
    mimeType,
    exists,
    isDirectory,
    path,
  )
}

@Composable
private fun FileError.displayText(): String {
  return message ?: stringResource(Res.string.unknown_error)
}

private suspend fun buildFileInfo(file: PlatformFile): FileInfo {
  return FileInfo(
    name = file.name,
    size = file.size(),
    mimeType = file.mimeType() ?: "-",
    exists = file.exists(),
    isDirectory = file.isDirectory(),
    path = file.path ?: "-",
  )
}
