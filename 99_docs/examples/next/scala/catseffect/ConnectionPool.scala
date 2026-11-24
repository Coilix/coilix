package com.example
package catseffect

import java.util.concurrent.atomic.AtomicBoolean

class ConnectionPool(val url: String) {
  private val open = AtomicBoolean(true)

  def isOpen: Boolean = open.get

  def close(): Unit = open.set(false)
}
