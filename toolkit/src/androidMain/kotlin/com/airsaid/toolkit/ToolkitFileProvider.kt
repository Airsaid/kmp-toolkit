package com.airsaid.toolkit

import androidx.core.content.FileProvider

/**
 * Toolkit-owned provider class used to keep manifest merging isolated from host app providers.
 */
class ToolkitFileProvider : FileProvider()
