package dev.coilix.kafka

import dev.coilix.config.valuetype.Secret
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs

/**
 * The security protocol and the credentials of a Kafka client. Passwords and the JAAS configuration are
 * [[dev.coilix.config.valuetype.Secret]]s, so they do not show in logs.
 *
 * @param securityProtocol
 *   how the client talks to the brokers
 * @param saslMechanism
 *   the SASL mechanism, such as `PLAIN` or `SCRAM-SHA-512`
 * @param saslJaasConfig
 *   the JAAS configuration with the SASL credentials
 * @param sslTruststoreLocation
 *   the trust store with the certificates of the brokers
 * @param sslTruststorePassword
 *   the password of the trust store
 * @param sslTruststoreType
 *   the type of the trust store, such as `JKS` or `PKCS12`
 * @param sslKeystoreLocation
 *   the key store with the certificate of the client
 * @param sslKeystorePassword
 *   the password of the key store
 * @param sslKeystoreType
 *   the type of the key store
 * @param sslKeyPassword
 *   the password of the private key
 * @param sslEndpointIdentificationAlgorithm
 *   the check of the host name of the brokers; an empty text turns it off
 */
case class KafkaAuthSettings(
  securityProtocol:                   SecurityProtocol = SecurityProtocol.Plaintext,
  saslMechanism:                      Option[String]   = None,
  saslJaasConfig:                     Option[Secret]   = None,
  sslTruststoreLocation:              Option[String]   = None,
  sslTruststorePassword:              Option[Secret]   = None,
  sslTruststoreType:                  Option[String]   = None,
  sslKeystoreLocation:                Option[String]   = None,
  sslKeystorePassword:                Option[Secret]   = None,
  sslKeystoreType:                    Option[String]   = None,
  sslKeyPassword:                     Option[Secret]   = None,
  sslEndpointIdentificationAlgorithm: Option[String]   = None
) {

  /** The Kafka properties of these settings. */
  def properties: Map[String, String] =
    Map("security.protocol" -> securityProtocol.kafkaName) ++ List(
      saslMechanism.map(SaslConfigs.SASL_MECHANISM -> _),
      saslJaasConfig.map(SaslConfigs.SASL_JAAS_CONFIG -> _.value),
      sslTruststoreLocation.map(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG -> _),
      sslTruststorePassword.map(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG -> _.value),
      sslTruststoreType.map(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG -> _),
      sslKeystoreLocation.map(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG -> _),
      sslKeystorePassword.map(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG -> _.value),
      sslKeystoreType.map(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG -> _),
      sslKeyPassword.map(SslConfigs.SSL_KEY_PASSWORD_CONFIG -> _.value),
      sslEndpointIdentificationAlgorithm.map(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG -> _)
    ).flatten
}
