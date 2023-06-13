package org.taymyr.lagom.ws

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.ws.StandaloneWSResponse
import play.api.libs.ws.WSRequestExecutor
import play.api.libs.ws.WSRequestFilter
import play.api.libs.ws.ahc.CurlFormat
import play.api.libs.ws.ahc.StandaloneAhcWSRequest

import java.util.UUID
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext

class AhcRequestResponseLogger(loggingSettings: LoggingSettings, logger: Logger)(implicit ec: ExecutionContext)
    extends WSRequestFilter
    with CurlFormat {

  override def apply(executor: WSRequestExecutor): WSRequestExecutor = WSRequestExecutor { request =>
    val eventualResponse = executor(request)
    val correlationId    = randomUUID()
    val r                = request.asInstanceOf[StandaloneAhcWSRequest]
    val url              = r.buildRequest().getUrl
    if (loggingSettings.skipUrls.exists { _.findFirstIn(url).nonEmpty }) {
      eventualResponse
    } else {
      logRequest(r, url, correlationId)
      eventualResponse.map { response =>
        logResponse(response, url, correlationId)
        response
      }
    }
  }

  private def logRequest(request: StandaloneAhcWSRequest, url: String, correlationId: UUID): Unit = {
    val sb = new StringBuilder(s"Request to $url")
    sb.append("\n")
      .append(s"Request correlation ID: $correlationId")
      .append("\n")
      .append(toCurl(request))
    logger.info(sb.toString())
  }

  private def logResponse(response: StandaloneWSResponse, url: String, correlationId: UUID): Unit = {
    val sb = new StringBuilder(s"Response from $url")
    sb.append("\n")
      .append(s"Request correlation ID: $correlationId")
      .append("\n")
      .append(s"Response actual URI: ${response.uri}")
      .append("\n")
      .append(s"Response status code: ${response.status}")
      .append("\n")
      .append(s"Response status text: ${response.statusText}")
      .append("\n")

    if (response.headers.nonEmpty) {
      sb.append("Response headers:")
        .append("\n")
      response.headers.foreach {
        case (header, values) =>
          values.foreach { value =>
            sb.append(s"    $header: $value")
            sb.append("\n")
          }
      }
    }

    if (response.cookies.nonEmpty) {
      sb.append("Response cookies:")
      response.cookies.foreach { cookie =>
        sb.append(s"    ${cookie.name}: ${cookie.value}")
        sb.append("\n")
      }
    }

    Option(response.body) match {
      case Some(body) =>
        sb.append("Response body: ").append(body)
      case None => // do nothing
    }

    logger.info(sb.toString())
  }
}

object AhcRequestResponseLogger {

  private val logger = LoggerFactory.getLogger("org.taymyr.lagom.ws.AhcRequestResponseLogger")

  def apply(loggingSettings: LoggingSettings)(implicit ec: ExecutionContext): AhcRequestResponseLogger =
    new AhcRequestResponseLogger(loggingSettings, logger)

  def apply(loggingSettings: LoggingSettings, logger: Logger)(implicit ec: ExecutionContext): AhcRequestResponseLogger =
    new AhcRequestResponseLogger(loggingSettings, logger)
}
