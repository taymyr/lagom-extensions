package org.taymyr.lagom.ws

import akka.util.ByteString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.taymyr.lagom.ws.AhcRequestResponseLogger.MDC_CORRELATION_ID
import org.taymyr.lagom.ws.AhcRequestResponseLogger.MDC_DURATION_MS
import org.taymyr.lagom.ws.AhcRequestResponseLogger.MDC_METHOD
import org.taymyr.lagom.ws.AhcRequestResponseLogger.MDC_REQUEST_BODY
import org.taymyr.lagom.ws.AhcRequestResponseLogger.MDC_RESPONSE_BODY
import org.taymyr.lagom.ws.AhcRequestResponseLogger.MDC_STATUS_CODE
import org.taymyr.lagom.ws.AhcRequestResponseLogger.MDC_URL
import play.api.libs.ws.EmptyBody
import play.api.libs.ws.InMemoryBody
import play.api.libs.ws.StandaloneWSResponse
import play.api.libs.ws.WSRequestExecutor
import play.api.libs.ws.WSRequestFilter
import play.api.libs.ws.ahc.CurlFormat
import play.api.libs.ws.ahc.StandaloneAhcWSRequest
import play.shaded.ahc.org.asynchttpclient.util.HttpUtils

import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext

class AhcRequestResponseLogger(loggingSettings: LoggingSettings, logger: Logger)(implicit ec: ExecutionContext)
    extends WSRequestFilter
    with CurlFormat {

  override def apply(executor: WSRequestExecutor): WSRequestExecutor = WSRequestExecutor { request =>
    val startTime        = System.nanoTime()
    val eventualResponse = executor(request)
    val correlationId    = randomUUID()
    val r                = request.asInstanceOf[StandaloneAhcWSRequest]
    val url              = r.buildRequest().getUrl
    if (loggingSettings.skipUrls.exists { _.findFirstIn(url).nonEmpty }) {
      eventualResponse
    } else {
      val mdc = loggingSettings.mdc.presets
        .find { _.urls.exists { _.findFirstIn(url).nonEmpty } }
        .getOrElse(loggingSettings.mdc.defaultPreset)
      if (loggingSettings.requestResponseCombined) {
        eventualResponse.map { response =>
          val durationMs = (System.nanoTime() - startTime) / 1000000
          logRequestResponseCombined(r, response, url, correlationId, durationMs, findPreset(response, url), mdc)
          response
        }
      } else {
        eventualResponse.map { response =>
          val preset = findPreset(response, url)
          logRequest(r, r.method, url, correlationId, preset, mdc)
          val durationMs = (System.nanoTime() - startTime) / 1000000
          logResponse(response, r.method, url, correlationId, durationMs, preset, mdc)
          response
        }
      }
    }
  }

  private def findPreset(response: StandaloneWSResponse, url: String): LoggingPreset = {
    val httpStatus = response.status
    val preset = loggingSettings.presets
      .find { p =>
        p.urls.exists { _.findFirstIn(url).nonEmpty } && p.httpCodes.contains(httpStatus)
      }
      .getOrElse(
        loggingSettings.presets
          .find { pr =>
            pr.urls.exists { _.findFirstIn(url).nonEmpty } && pr.httpCodes.isEmpty
          }
          .getOrElse(loggingSettings.defaultPreset)
      )
    preset
  }

  private def logRequest(
      request: StandaloneAhcWSRequest,
      method: String,
      url: String,
      correlationId: UUID,
      preset: LoggingPreset,
      mdc: MdcPreset
  ): Unit = {
    if (mdc.enabled) {
      fillMdcCommonContext(method, url, correlationId, mdc)
      fillMdcRequestContext(request, mdc)
    }
    logger.info(createRequestLogMessage(request, url, correlationId, preset))
    if (mdc.enabled) {
      clearMdcContext()
    }
  }

  private def logResponse(
      response: StandaloneWSResponse,
      method: String,
      url: String,
      correlationId: UUID,
      duration: Long,
      preset: LoggingPreset,
      mdc: MdcPreset
  ): Unit = {
    if (mdc.enabled) {
      fillMdcCommonContext(method, url, correlationId, mdc)
      fillMdcResponseContext(response, duration, mdc)
    }
    logger.info(createResponseLogMessage(response, url, preset, Option(correlationId)))
    if (mdc.enabled) {
      clearMdcContext()
    }
  }

  private def logRequestResponseCombined(
      request: StandaloneAhcWSRequest,
      response: StandaloneWSResponse,
      url: String,
      correlationId: UUID,
      durationMs: Long,
      preset: LoggingPreset,
      mdc: MdcPreset,
  ): Unit = {
    val req  = createRequestLogMessage(request, url, correlationId, preset)
    val resp = createResponseLogMessage(response, url, preset)

    if (mdc.enabled) {
      fillMdcCommonContext(request.method, url, correlationId, mdc)
      fillMdcRequestContext(request, mdc)
      fillMdcResponseContext(response, durationMs, mdc)
    }
    logger.info(s"$req\n$resp")
    if (mdc.enabled) {
      clearMdcContext()
    }
  }

  private def createRequestLogMessage(
      request: StandaloneAhcWSRequest,
      url: String,
      correlationId: UUID,
      preset: LoggingPreset
  ): String = {
    val sb = new StringBuilder(s"Request to $url\n")
    sb.append(s"Request correlation ID: $correlationId")
      .append("\n")
    if (preset.requestElements.contains("curl")) {
      sb.append(toCurl(request))
        .append("\n")
    }
    if (preset.requestElements.contains("headers") && request.headers.nonEmpty) {
      sb.append("Request headers:")
        .append("\n")
      request.headers.foreach {
        case (header, values) =>
          values.foreach { value =>
            sb.append(s"    $header: $value")
            sb.append("\n")
          }
      }
    }
    if (preset.requestElements.contains("cookies") && request.cookies.nonEmpty) {
      sb.append("Request cookies:")
        .append("\n")
      request.cookies.foreach { cookie =>
        sb.append(s"    ${cookie.name}: ${cookie.value}")
        sb.append("\n")
      }
    }
    if (preset.requestElements.contains("body")) {
      request.body match {
        case InMemoryBody(byteString) =>
          sb.append("Request body: ").append(byteString.decodeString(findCharset(request)))
        case EmptyBody => // Do nothing.
        case other     => // Do nothing.
      }
    }
    sb.toString()
  }

  private def createResponseLogMessage(
      response: StandaloneWSResponse,
      url: String,
      preset: LoggingPreset,
      correlationId: Option[UUID] = Option.empty,
  ): String = {
    val sb = new StringBuilder(s"Response from $url\n")
    correlationId.foreach(cid =>
      sb.append(s"Request correlation ID: $cid")
        .append("\n")
    )
    sb.append(s"Response actual URI: ${response.uri}")
      .append("\n")
      .append(s"Response status code: ${response.status}")
      .append("\n")
      .append(s"Response status text: ${response.statusText}")
      .append("\n")

    if (preset.responseElements.contains("headers") && response.headers.nonEmpty) {
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

    if (preset.responseElements.contains("cookies") && response.cookies.nonEmpty) {
      sb.append("Response cookies:")
        .append("\n")
      response.cookies.foreach { cookie =>
        sb.append(s"    ${cookie.name}: ${cookie.value}")
        sb.append("\n")
      }
    }

    if (preset.responseElements.contains("body")) {
      Option(response.body) match {
        case Some(body) =>
          sb.append("Response body: ").append(body)
        case None => // do nothing
      }
    }
    sb.toString()
  }

  private def fillMdcCommonContext(
      method: String,
      url: String,
      correlationId: UUID,
      mdc: MdcPreset
  ): Unit = {
    putMdc(mdc.fields, MDC_CORRELATION_ID, correlationId.toString)
    putMdc(mdc.fields, MDC_METHOD, method)
    putMdc(mdc.fields, MDC_URL, url)
  }

  private def fillMdcRequestContext(request: StandaloneAhcWSRequest, mdc: MdcPreset): Unit = {
    request.body match {
      case InMemoryBody(byteString) =>
        putMdc(
          mdc.fields,
          MDC_REQUEST_BODY,
          () => {
            val charset = findCharset(request)
            mdc.requestBodyMaxBytes
              .map(
                truncate(byteString, charset, _)
              )
              .getOrElse(byteString)
              .decodeString(charset)
          }
        )
      case EmptyBody => // Do nothing.
      case other     => // Do nothing.
    }
  }

  private def fillMdcResponseContext(
      response: StandaloneWSResponse,
      duration: Long,
      mdc: MdcPreset
  ): Unit = {
    putMdc(mdc.fields, MDC_STATUS_CODE, response.status.toString)
    putMdc(mdc.fields, MDC_DURATION_MS, duration.toString)

    if (mdc.fields.contains(MDC_RESPONSE_BODY)) {
      Option(response.bodyAsBytes) match {
        case Some(byteString) =>
          putMdc(
            mdc.fields,
            MDC_RESPONSE_BODY,
            () => {
              val charset = findCharset(response)
              mdc.responseBodyMaxBytes
                .map(
                  truncate(byteString, charset, _)
                )
                .getOrElse(byteString)
                .decodeString(charset)
            }
          )
        case None => // do nothing
      }
    }
  }

  private def clearMdcContext(): Unit = {
    MDC.remove(mdcKey(MDC_CORRELATION_ID))
    MDC.remove(mdcKey(MDC_METHOD))
    MDC.remove(mdcKey(MDC_URL))
    MDC.remove(mdcKey(MDC_STATUS_CODE))
    MDC.remove(mdcKey(MDC_DURATION_MS))
    MDC.remove(mdcKey(MDC_REQUEST_BODY))
    MDC.remove(mdcKey(MDC_RESPONSE_BODY))
  }

  private def putMdc(fields: Seq[String], key: String, value: () => String): Unit = {
    if (fields.contains(key)) {
      MDC.put(mdcKey(key), value())
    }
  }

  private def putMdc(fields: Seq[String], key: String, value: String): Unit = {
    putMdc(fields, key, () => value)
  }

  private def mdcKey(name: String): String = {
    val key = loggingSettings.mdc.nameMappings.getOrElse(name, name)
    s"${loggingSettings.mdc.namePrefix}$key"
  }

  private def findCharset(request: StandaloneWSResponse): String = {
    Option(HttpUtils.extractContentTypeCharsetAttribute(request.contentType))
      .getOrElse {
        StandardCharsets.UTF_8
      }
      .name()
  }

  private def truncate(body: ByteString, charset: String, maxBytes: Int) = {
    if (maxBytes > 0 && body.size > maxBytes) {
      body
        .take(maxBytes)
        .concat(ByteString("(message truncated to " + maxBytes + " bytes)", charset))
    } else {
      body
    }
  }
}

object AhcRequestResponseLogger {

  private val logger             = LoggerFactory.getLogger("org.taymyr.lagom.ws.AhcRequestResponseLogger")
  private val MDC_CORRELATION_ID = "correlation-id"
  private val MDC_METHOD         = "method"
  private val MDC_URL            = "url"
  private val MDC_STATUS_CODE    = "status-code"
  private val MDC_DURATION_MS    = "duration-ms"
  private val MDC_REQUEST_BODY   = "request-body"
  private val MDC_RESPONSE_BODY  = "response-body"

  def apply(loggingSettings: LoggingSettings)(implicit ec: ExecutionContext): AhcRequestResponseLogger =
    new AhcRequestResponseLogger(loggingSettings, logger)

  def apply(loggingSettings: LoggingSettings, logger: Logger)(implicit ec: ExecutionContext): AhcRequestResponseLogger =
    new AhcRequestResponseLogger(loggingSettings, logger)
}
