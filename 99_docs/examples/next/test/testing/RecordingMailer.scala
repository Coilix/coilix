package com.example
package testing

import java.util.concurrent.atomic.AtomicReference

final class RecordingMailer extends Mailer {
  private val mails = AtomicReference(List.empty[(String, String)])

  override def send(to: String, text: String): Unit = {
    val _ = mails.updateAndGet(_ :+ (to -> text))
  }

  def sent: List[(String, String)] = mails.get
}
