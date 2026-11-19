package com.example
package testing

trait Mailer {
  def send(to: String, text: String): Unit
}
