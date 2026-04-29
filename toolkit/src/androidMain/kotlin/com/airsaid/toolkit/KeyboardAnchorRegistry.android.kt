package com.airsaid.toolkit

import android.view.View

internal object KeyboardAnchorRegistry {

  private val lock = Any()
  private val listeners = LinkedHashSet<(KeyboardAnchorEvent) -> Unit>()

  fun updateAnchor(view: View) {
    dispatch(KeyboardAnchorEvent.Available(view))
  }

  fun clearAnchor(view: View) {
    dispatch(KeyboardAnchorEvent.Unavailable(view))
  }

  fun register(listener: (KeyboardAnchorEvent) -> Unit) {
    synchronized(lock) {
      listeners.add(listener)
    }
  }

  fun unregister(listener: (KeyboardAnchorEvent) -> Unit) {
    synchronized(lock) {
      listeners.remove(listener)
    }
  }

  private fun dispatch(event: KeyboardAnchorEvent) {
    val snapshot = synchronized(lock) { listeners.toList() }
    snapshot.forEach { it(event) }
  }
}

internal sealed interface KeyboardAnchorEvent {
  data class Available(val view: View) : KeyboardAnchorEvent
  data class Unavailable(val view: View) : KeyboardAnchorEvent
}
