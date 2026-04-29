package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.error_format
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ToolkitDemoPage(
  descriptionRes: StringResource,
  codeRes: StringResource,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val scrollState = rememberScrollState()
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
      .verticalScroll(scrollState),
  ) {
    Text(
      text = stringResource(descriptionRes),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(top = 6.dp)
    )
    Card(
      colors = CardDefaults.cardColors(),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.padding(top = 16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        content()
      }
    }
    CodeBlock(code = stringResource(codeRes), modifier = Modifier.padding(top = 16.dp))
  }
}

@Composable
internal fun CodeBlock(
  code: String,
  modifier: Modifier = Modifier,
) {
  SelectionContainer {
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(12.dp),
      modifier = modifier.fillMaxWidth()
    ) {
      Text(
        text = code,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(12.dp)
      )
    }
  }
}

@Composable
internal fun StatusText(value: String?) {
  Text(
    text = value ?: "-",
    style = MaterialTheme.typography.bodySmall,
    modifier = Modifier.padding(top = 12.dp)
  )
}

@Composable
internal fun ErrorText(message: String?) {
  if (message.isNullOrBlank()) return
  Text(
    text = stringResource(Res.string.error_format, message),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.error,
    modifier = Modifier.padding(top = 8.dp)
  )
}
