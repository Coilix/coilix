package com.example

import dev.coilix.annotation.ConfigurationProperties
import dev.coilix.config.valuetype.Secret

@ConfigurationProperties("shop")
final case class ShopSettings(port: Port, mode: ShopMode, paymentKey: Secret, backupPorts: List[Port] = Nil)
