package org.taymyr.akka.cluster.sharding.typed.javadsl

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.javadsl.EntityRef
import akka.pattern.StatusReply
import kotlinx.coroutines.future.await
import java.time.Duration

object ClusterSharding {

    /**
     * Kotlin DSL inline wrapper. For more detail see [EntityRef.ask]
     */
    suspend inline fun <M, RES> EntityRef<M>.ask(timeout: Duration, noinline message: (ActorRef<RES>) -> M): RES = this.ask(message, timeout).await()

    /**
     * Kotlin DSL inline wrapper. For more detail see [EntityRef.askWithStatus]
     */
    suspend inline fun <M, RES> EntityRef<M>.askWithStatus(timeout: Duration, noinline message: (ActorRef<StatusReply<RES>>) -> M): RES = this.askWithStatus(message, timeout).await()
}
