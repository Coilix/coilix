package com.example
package tasks

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

final class FlakyMailSender extends MailSender {
  private val calls = AtomicInteger(0)

  val delivered: CountDownLatch = CountDownLatch(1)

  override def send(to: String, text: String): Unit =
    if (calls.incrementAndGet() == 1) throw IllegalStateException("mail server busy") else delivered.countDown()
}
