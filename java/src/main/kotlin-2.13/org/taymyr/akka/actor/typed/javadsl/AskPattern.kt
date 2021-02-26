package org.taymyr.akka.actor.typed.javadsl

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.RecipientRef
import akka.actor.typed.javadsl.Adapter
import akka.actor.typed.javadsl.AskPattern
import akka.japi.function.Function
import akka.pattern.StatusReply
import kotlinx.coroutines.future.await
import java.time.Duration

/**
 * Extension of [AskPattern].
 */
object AskPattern {

    /**
     *
     * Send a message to the Actor referenced by this [RecipientRef] and expect an answer.
     * @param Req – The request protocol, what the other actor accepts
     * @param Res – The response protocol, what the other actor sends back
     * @param system Actor system
     * @param timeout Response timeout
     * @param messageFactory Message factory
     * @return Res
     */
    suspend fun <Req, Res> RecipientRef<Req>.ask(
        system: ActorSystem,
        timeout: Duration,
        messageFactory: Function<ActorRef<Res>, Req>
    ): Res = AskPattern.ask(this, messageFactory, timeout, Adapter.toTyped(system).scheduler()).await()

    /**
     *
     * Send a message to the Actor referenced by this [RecipientRef] and expect an answer that of type [StatusReply].
     * @param Req – The request protocol, what the other actor accepts
     * @param Res – The response protocol, what the other actor sends back
     * @param system Actor system
     * @param timeout Response timeout
     * @param messageFactory Message factory
     * @return Res
     */
    suspend fun <Req, Res> RecipientRef<Req>.askWithStatus(
        system: ActorSystem,
        timeout: Duration,
        messageFactory: Function<ActorRef<StatusReply<Res>>, Req>
    ): Res = AskPattern.askWithStatus(this, messageFactory, timeout, Adapter.toTyped(system).scheduler()).await()
}
