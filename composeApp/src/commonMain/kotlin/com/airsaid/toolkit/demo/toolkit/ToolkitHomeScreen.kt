package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToolkitHomeScreen(
  modifier: Modifier = Modifier,
  onNavigate: (route: String) -> Unit,
) {
  val items = remember { ToolkitDemoItems.all }
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items.forEach { item ->
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp),
        onClick = { onNavigate(item.route) },
      ) {
        Text(item.title)
      }
    }
  }
}
