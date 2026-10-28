package org.scaling.context.configured

import org.scaling.annotation.ConfigurationProperties

@ConfigurationProperties("server")
final case class ServerSettings(host: String, port: Int = 80)
