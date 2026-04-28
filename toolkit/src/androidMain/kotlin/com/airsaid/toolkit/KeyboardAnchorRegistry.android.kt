package com.airsaid.toolkit

import android.view.View

internal object KeyboardAnchorRegistry {

  private val lock = Any()
  private var anchorView: View? = null
  private val listeners = LinkedHashSet<(View?) -> Unit>()

  fun updateAnchor(view: View?) {
    val snapshot = synchronized(lock) {
      if (anchorView === view) return
      anchorView = view
      listeners.toList()
    }
    snapshot.forEach { it(view) }
  }

  fun getAnchor(): View? {
    return synchronized(lock) { anchorView }
  }

  fun register(listener: (View?) -> Unit): View? {
    return synchronized(lock) {
      listeners.add(listener)
      anchorView
    }
  }

  fun unregister(listener: (View?) -> Unit) {
    synchronized(lock) {
      listeners.remove(listener)
    }
  }
}
