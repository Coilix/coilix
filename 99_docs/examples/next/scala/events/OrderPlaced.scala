package com.example
package events

final case class OrderPlaced(orderId: String, customer: String, total: BigDecimal)
