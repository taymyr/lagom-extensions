package org.taymyr.lagom.ws

import com.typesafe.config.Config
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

  private val isLogging = config.getBoolean("configured-ahc-ws-client.logging.enabled")

  override def url(url: String): WSRequest = {
    val wSRequest = super.url(url)
    if (isLogging) {
      wSRequest.withRequestFilter(AhcRequestResponseLogger())
    } else {
      wSRequest
    }
  }
}

object ConfiguredAhcWSClient {
  def apply(underlyingClient: StandaloneAhcWSClient, config: Config)(
      implicit executionContext: ExecutionContext
  ): ConfiguredAhcWSClient =
    new ConfiguredAhcWSClient(underlyingClient, config)
}
