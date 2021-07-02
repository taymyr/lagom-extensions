package org.taymyr.lagom.javadsl.api

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.api.Service
import com.lightbend.lagom.javadsl.api.Service.named
import com.lightbend.lagom.javadsl.api.Service.restCall
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.Method
import kotlinx.coroutines.Dispatchers
import org.pac4j.core.client.BaseClient
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.AnonymousCredentials
import org.pac4j.core.credentials.Credentials
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.exception.HttpAction
import org.pac4j.core.exception.TechnicalException
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.redirect.RedirectAction
import kotlin.reflect.jvm.javaMethod

abstract class TestService : Service, CoroutineSecuredService {

    private class TestClient : BaseClient<Credentials, CommonProfile>() {
        override fun internalInit() {}
        override fun redirect(context: WebContext?): HttpAction = HttpAction.redirect(context, "/")
        override fun getLogoutAction(
            context: WebContext?,
            currentProfile: CommonProfile?,
            targetUrl: String?
        ): RedirectAction = RedirectAction.redirect("/")
        override fun getCredentials(context: WebContext?): Credentials {
            fun isAnonymous(): Boolean = try {
                context?.getRequestHeader("anonymous") != null
            } catch (e: TechnicalException) {
                false
            }
            return if (isAnonymous()) AnonymousCredentials()
            else UsernamePasswordCredentials("login", "password").apply { userProfile = CommonProfile() }
        }
    }

    // Do not use an object because client.name returns an empty string
    private val client = TestClient()
    private val pac4jConfig = Config(client).apply {
        clients.defaultSecurityClients = client.name
    }

    override val dispatcher = Dispatchers.Unconfined

    override fun getSecurityConfig(): Config = pac4jConfig

    abstract fun testMethod(): ServiceCall<NotUsed, String>

    override fun descriptor(): Descriptor {
        return named("test-service")
            .withCalls(
                restCall<NotUsed, String>(Method.GET, "/test", TestService::testMethod.javaMethod)
            )
    }
}
