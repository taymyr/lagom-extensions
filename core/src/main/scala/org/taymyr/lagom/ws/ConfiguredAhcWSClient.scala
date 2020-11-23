package org.taymyr.lagom.ws

import java.lang.Math.max

import com.typesafe.config.Config
import org.taymyr.lagom.ws.ConfiguredAhcWSClient.loggingConfigPath
import org.taymyr.lagom.ws.ConfiguredAhcWSClient.maxCharsMinThreshold
import play.api.libs.ws.WSRequest
import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.ExecutionContext

/**
 * Extension of AhcWSClient with logging.
 */
class ConfiguredAhcWSClient(underlyingClient: StandaloneAhcWSClient, config: Config)(
    implicit executionContext: ExecutionContext
) extends AhcWSClient(underlyingClient = underlyingClient) {

  private val isLogging = config.getBoolean(s"$loggingConfigPath.enabled")
  private val maxChars  = config.getInt(s"$loggingConfigPath.max-chars")

  override def url(url: String): WSRequest = {
    val wSRequest = super.url(url)
    if (isLogging) {
      wSRequest.withRequestFilter(AhcRequestResponseLogger(max(maxChars, maxCharsMinThreshold)))
    } else {
      wSRequest
    }
  }
}

object ConfiguredAhcWSClient {
  private val loggingConfigPath    = "configured-ahc-ws-client.logging"
  private val maxCharsMinThreshold = 256
  def apply(underlyingClient: StandaloneAhcWSClient, config: Config)(
      implicit executionContext: ExecutionContext
  ): ConfiguredAhcWSClient =
    new ConfiguredAhcWSClient(underlyingClient, config)
}
