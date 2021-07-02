package org.taymyr.lagom.javadsl.api

import akka.NotUsed
import akka.japi.Pair
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader
import com.lightbend.lagom.javadsl.server.HeaderServiceCall
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.pac4j.core.profile.AnonymousProfile
import org.pac4j.core.profile.CommonProfile

class CoroutineServiceTest {

    @Test
    @DisplayName("serviceCall should works")
    fun testServiceCall() {
        val expectedResult = "result"
        val simpleService = object : TestService() {
            override fun testMethod(): ServiceCall<NotUsed, String> = serviceCall {
                expectedResult
            }
        }
        runBlocking {
            val actualResult = simpleService.testMethod()
                .invoke().await()
            assertThat(actualResult).isEqualTo(expectedResult)
        }
    }

    @Test
    @DisplayName("headerServiceCall should works")
    fun testHeaderServiceCall() {
        val expectedResult = "result"
        val expectedHeaderName = "headerName"
        val expectedHeaderValue = "headerValue"
        val simpleService = object : TestService() {
            override fun testMethod(): HeaderServiceCall<NotUsed, String> = headerServiceCall { headers, _ ->
                val expectedHeader = headers.getHeader(expectedHeaderName)
                assertThat(expectedHeader.isPresent)
                assertThat(expectedHeader.get()).isEqualTo(expectedHeaderValue)
                Pair.create(ResponseHeader.OK, expectedResult)
            }
        }
        runBlocking {
            val actualResult = simpleService.testMethod()
                .handleRequestHeader { headers -> headers.withHeader(expectedHeaderName, expectedHeaderValue) }
                .await()
            assertThat(actualResult).isEqualTo(expectedResult)
        }
    }

    @Test
    @DisplayName("authorizedServiceCall should works")
    fun testAuthorizedServiceCall() {
        val expectedResult = "result"
        val simpleService = object : TestService() {
            override fun testMethod(): ServiceCall<NotUsed, String> = authorizedServiceCall { _, profile ->
                assertThat(profile).isInstanceOf(CommonProfile::class.java)
                expectedResult
            }
        }
        runBlocking {
            val actualResult = simpleService.testMethod()
                .handleRequestHeader { headers -> headers }
                .await()
            assertThat(actualResult).isEqualTo(expectedResult)
        }
    }

    @Test
    @DisplayName("authorizedHeaderServiceCall should works")
    fun testAuthorizedHeaderServiceCall() {
        val expectedResult = "result"
        val expectedHeaderName = "headerName"
        val expectedHeaderValue = "headerValue"
        val simpleService = object : TestService() {
            override fun testMethod(): ServiceCall<NotUsed, String> =
                authorizedHeaderServiceCall { headers, _, profile ->
                    assertThat(profile).isInstanceOf(CommonProfile::class.java)
                    val expectedHeader = headers.getHeader(expectedHeaderName)
                    assertThat(expectedHeader.isPresent)
                    assertThat(expectedHeader.get()).isEqualTo(expectedHeaderValue)
                    Pair.create(ResponseHeader.OK, expectedResult)
                }
        }
        runBlocking {
            val actualResult = simpleService.testMethod()
                .handleRequestHeader { headers -> headers.withHeader(expectedHeaderName, expectedHeaderValue) }
                .await()
            assertThat(actualResult).isEqualTo(expectedResult)
        }
    }

    @Test
    @DisplayName("authenticateServiceCall should works")
    fun testAuthenticateServiceCall() {
        val expectedResult = "result"
        val simpleService = object : TestService() {
            override fun testMethod(): ServiceCall<NotUsed, String> = authenticatedServiceCall { _, profile ->
                assertThat(profile).isInstanceOf(CommonProfile::class.java)
                expectedResult
            }
        }
        runBlocking {
            val actualResult = simpleService.testMethod()
                .handleRequestHeader { headers -> headers }
                .await()
            assertThat(actualResult).isEqualTo(expectedResult)
        }
    }

    @Test
    @DisplayName("authenticateHeaderServiceCall should works")
    fun testAuthenticateHeaderServiceCall() {
        val expectedResult = "result"
        val expectedHeaderName = "headerName"
        val expectedHeaderValue = "headerValue"
        val simpleService = object : TestService() {
            override fun testMethod(): ServiceCall<NotUsed, String> =
                authenticatedHeaderServiceCall { headers, _, profile ->
                    assertThat(profile).isInstanceOf(AnonymousProfile::class.java)
                    val expectedHeader = headers.getHeader(expectedHeaderName)
                    assertThat(expectedHeader.isPresent)
                    assertThat(expectedHeader.get()).isEqualTo(expectedHeaderValue)
                    Pair.create(ResponseHeader.OK, expectedResult)
                }
        }
        runBlocking {
            val actualResult = simpleService.testMethod()
                .handleRequestHeader {
                    it.withHeader(expectedHeaderName, expectedHeaderValue).withHeader("anonymous", "yes")
                }
                .await()
            assertThat(actualResult).isEqualTo(expectedResult)
        }
    }
}
