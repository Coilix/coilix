package dev.coilix.kafka

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

/** How a Kafka client talks to the brokers. The configuration writes the names in kebab case, such as `sasl-ssl`. */
@Enumerated(ignoreCase = true)
enum SecurityProtocol(val kafkaName: String) {

  /** Without encryption and authentication. */
  @EnumName("plaintext") case Plaintext extends SecurityProtocol("PLAINTEXT")

  /** Encrypted with TLS, optionally with a client certificate. */
  @EnumName("ssl") case Ssl extends SecurityProtocol("SSL")

  /** Authenticated with SASL without encryption. */
  @EnumName("sasl-plaintext") case SaslPlaintext extends SecurityProtocol("SASL_PLAINTEXT")

  /** Authenticated with SASL and encrypted with TLS. */
  @EnumName("sasl-ssl") case SaslSsl extends SecurityProtocol("SASL_SSL")
}
