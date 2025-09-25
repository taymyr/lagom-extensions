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

case class LoggingSettings(
    enabled: Boolean,
    skipUrls: Seq[Regex],
    defaultPreset: LoggingPreset,
    presets: Seq[LoggingPreset]
)

case class LoggingPreset(
    requestElements: Seq[String],
    responseElements: Seq[String],
    urls: Seq[Regex]
)

object LoggingSettings {

  def apply(config: Config): LoggingSettings = LoggingSettings(
    config.getBoolean("enabled"),
    buildStrings(config, "skip-urls").map { _.r },
    buildDefaultPreset(config),
    buildPresets(config),
  )

  def buildDefaultPreset(config: Config): LoggingPreset = {
    LoggingPreset(
      requestElements = buildStrings(config, "default-preset.request-elements"),
      responseElements = buildStrings(config, "default-preset.response-elements"),
      urls = Seq.empty[Regex]
    )
  }

  def buildPresets(config: Config): Seq[LoggingPreset] = {
    config
      .getConfigList("presets")
      .asScala
      .map { cfg =>
        LoggingPreset(
          requestElements = buildStrings(cfg, "request-elements"),
          responseElements = buildStrings(cfg, "response-elements"),
          urls = buildStrings(cfg, "urls").map { _.r }
        )
      }
      .toSeq
  }

  def buildStrings(config: Config, param: String): Seq[String] = {
    config
      .getStringList(param)
      .asScala
      .toSeq
      .filter { s =>
        s != null && s.nonEmpty
      }
  }
}
