package dev.coilix.context.configured

import dev.coilix.annotation.ConfigurationProperties

@ConfigurationProperties("server")
final case class ServerSettings(host: String, port: Int = 80)
