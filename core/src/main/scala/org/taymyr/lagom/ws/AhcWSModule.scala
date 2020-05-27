package org.taymyr.lagom.ws

import akka.stream.Materializer
import com.typesafe.config.Config
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api.inject.SimpleModule
import play.api.inject.bind
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AsyncHttpClientProvider
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

import scala.concurrent.ExecutionContext

/**
 * A Play binding.
 */
class AhcWSModule
    extends SimpleModule(
      bind[AsyncHttpClient].toProvider[AsyncHttpClientProvider],
      bind[WSClient].toProvider[AhcWSClientProvider]
    ) {}

@Singleton
class AhcWSClientProvider @Inject() (asyncHttpClient: AsyncHttpClient, config: Config)(
    implicit materializer: Materializer,
    ec: ExecutionContext
) extends Provider[WSClient] {

  lazy val get: WSClient = ConfiguredAhcWSClient(new StandaloneAhcWSClient(asyncHttpClient), config)
}
