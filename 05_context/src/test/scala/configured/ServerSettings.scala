package io.github.coilix.context.configured

import io.github.coilix.annotation.ConfigurationProperties

@ConfigurationProperties("server")
final case class ServerSettings(host: String, port: Int = 80)
