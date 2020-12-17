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
    suspend fun <M, RES> EntityRef<M>.ask(timeout: Duration, message: (ActorRef<RES>) -> M): RES = this.ask(message, timeout).await()

    /**
     * Kotlin DSL inline wrapper. For more detail see [EntityRef.askWithStatus]
     */
    suspend fun <M, RES> EntityRef<M>.askWithStatus(timeout: Duration, message: (ActorRef<StatusReply<RES>>) -> M): RES = this.askWithStatus(message, timeout).await()
}
