package org.taymyr.akka.stream.javadsl

import akka.stream.javadsl.Flow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future

/**
 * Extension of [Flow].
 */
object Flow {

    /**
     * Transform this stream by applying the given suspend function to each of the elements as they pass through
     * this processing step. The function returns the value, which  will be emitted downstream.
     * The number of suspend function that shall run in parallel is given as the first argument to mapCoroutine.
     * These suspend function may complete in any order, but the elements that are emitted
     * downstream are in the same order as received from upstream.
     * @param parallelism The number of suspend function that shall run in parallel
     * @param dispatcher Coroutine dispatcher
     * @param block Suspend function
     * @return Flow
     */
    inline fun <reified In, reified Out, reified Mat, reified T> Flow<In, Out, Mat>.mapCoroutine(
        parallelism: Int = 1,
        dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
        crossinline block: suspend CoroutineScope.(Out) -> T
    ): Flow<In, T, Mat> {
        return this.mapAsync(parallelism) { CoroutineScope(dispatcher).future { block(it) } }
    }
}
