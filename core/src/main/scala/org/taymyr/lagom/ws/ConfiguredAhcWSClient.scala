package org.taymyr.lagom.ws

import com.typesafe.config.Config
import play.api.libs.ws.WSRequest
import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

/**
 * Extension of [[AhcWSClient]] with logging.
 */
class ConfiguredAhcWSClient(underlyingClient: StandaloneAhcWSClient, config: Config)(
    implicit executionContext: ExecutionContext
) extends AhcWSClient(underlyingClient = underlyingClient) {

  private val loggingSettings = LoggingSettings(config.getConfig("configured-ahc-ws-client.logging"))

  override def url(url: String): WSRequest =
    if (loggingSettings.enabled) {
      super.url(url).withRequestFilter(AhcRequestResponseLogger(loggingSettings))
    } else {
      super.url(url)
    }
}

object ConfiguredAhcWSClient {
  def apply(underlyingClient: StandaloneAhcWSClient, config: Config)(
      implicit executionContext: ExecutionContext
  ): ConfiguredAhcWSClient =
    new ConfiguredAhcWSClient(underlyingClient, config)
}

case class LoggingSettings(enabled: Boolean, skipUrls: Seq[Regex])

object LoggingSettings {

  def apply(config: Config): LoggingSettings = LoggingSettings(
    config.getBoolean("enabled"),
    config
      .getStringList("skip-urls")
      .asScala
      .toSeq
      .filter { s =>
        s != null && s.nonEmpty
      }
      .map { _.r }
  )
}
