package dev.coilix.logging

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMDCAdapter
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

/**
 * The SLF4J 2 provider of the `logging` module, found by SLF4J through `META-INF/services/org.slf4j.spi.SLF4JServiceProvider`.
 * Its loggers are created by [[dev.coilix.logging.CoilixLogging.factory]], and initializing it bridges `java.util.logging` to
 * SLF4J.
 */
final class CoilixServiceProvider extends SLF4JServiceProvider {

  private val markers = BasicMarkerFactory()

  private val context = BasicMDCAdapter()

  override def getLoggerFactory: ILoggerFactory = CoilixLogging.factory

  override def getMarkerFactory: IMarkerFactory = markers

  override def getMDCAdapter: MDCAdapter = context

  override def getRequestedApiVersion: String = "2.0.99"

  override def initialize(): Unit = {
    JulBridge.install()
    JulBridge.follow(CoilixLogging.factory.current)
  }
}
