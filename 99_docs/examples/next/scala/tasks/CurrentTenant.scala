package com.example
package tasks

object CurrentTenant {
  private val tenant = ThreadLocal.withInitial[Option[String]](() => None)

  def get: Option[String] = tenant.get

  def withTenant[T](value: Option[String])(body: => T): T = {
    val previous = tenant.get
    tenant.set(value)
    try {
      body
    } finally {
      tenant.set(previous)
    }
  }
}
