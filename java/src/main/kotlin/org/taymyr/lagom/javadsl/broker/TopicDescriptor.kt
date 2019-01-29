package org.taymyr.lagom.javadsl.broker

/**
 * The descriptor of Kafka topic.
 *
 * @param id Id (name) of a topic
 * @param type Type of a topic record
 * @param T Type of record
 */
data class TopicDescriptor<T> (val id: String, val type: Class<T>) {

    /**
     * Utilities for work with [TopicDescriptor].
     */
    companion object {

        /**
         * Instantiating [TopicDescriptor].
         *
         * @param id Id (name) of a topic
         * @param type Type of a topic record
         * @param T Type of a topic record
         * @return Topic descriptor
         */
        @JvmStatic
        fun <T> of(id: String, type: Class<T>): TopicDescriptor<T> = TopicDescriptor(id, type)
    }
}
