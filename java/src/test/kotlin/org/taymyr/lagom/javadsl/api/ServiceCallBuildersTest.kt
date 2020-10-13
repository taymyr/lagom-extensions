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
import org.pac4j.core.client.BaseClient
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.exception.HttpAction
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.redirect.RedirectAction
import org.pac4j.lagom.javadsl.SecuredService

class ServiceCallBuildersTest {

    @Test
    @DisplayName("serviceCall should works")
    fun testServiceCall() {
        val expectedResult = "result"
        val simpleService = object : TestService {
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
        val simpleService = object : TestService {
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
        val securityClient = object : BaseClient<UsernamePasswordCredentials, CommonProfile>() {
            override fun internalInit() { }
            override fun redirect(context: WebContext?): HttpAction = HttpAction.redirect(context, "/")
            override fun getLogoutAction(context: WebContext?, currentProfile: CommonProfile?, targetUrl: String?): RedirectAction = RedirectAction.redirect("/")
            override fun getCredentials(context: WebContext?): UsernamePasswordCredentials {
                val credentials = UsernamePasswordCredentials("login", "password")
                credentials.userProfile = CommonProfile()
                return credentials
            }
        }
        val securityConfig = Config(securityClient)
        securityConfig.clients.defaultSecurityClients = securityClient.name
        val expectedResult = "result"
        val simpleService = object : TestService, SecuredService {
            override fun getSecurityConfig(): Config = securityConfig
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
        val securityClient = object : BaseClient<UsernamePasswordCredentials, CommonProfile>() {
            override fun internalInit() { }
            override fun redirect(context: WebContext?): HttpAction = HttpAction.redirect(context, "/")
            override fun getLogoutAction(context: WebContext?, currentProfile: CommonProfile?, targetUrl: String?): RedirectAction = RedirectAction.redirect("/")
            override fun getCredentials(context: WebContext?): UsernamePasswordCredentials {
                val credentials = UsernamePasswordCredentials("login", "password")
                credentials.userProfile = CommonProfile()
                return credentials
            }
        }
        val securityConfig = Config(securityClient)
        securityConfig.clients.defaultSecurityClients = securityClient.name
        val expectedResult = "result"
        val expectedHeaderName = "headerName"
        val expectedHeaderValue = "headerValue"
        val simpleService = object : TestService, SecuredService {
            override fun getSecurityConfig(): Config = securityConfig
            override fun testMethod(): ServiceCall<NotUsed, String> = authorizedHeaderServiceCall { headers, _, profile ->
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
}
