package com.ververica.statefun.reqreply;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.kafka.requestreply.ReplyingKafkaOperations;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import java.time.Duration;

/**
 * Copied and modified from {@link ReplyingKafkaOperations}.
 */
public interface StatefunReplyingKafkaOperations<K, V, R> {

    /**
     * Send a request message and receive a reply message with the default timeout.
     * @param message the message to send.
     * @return a RequestReplyMessageFuture.
     * @since 2.7
     *
     */
    default StatefunRequestReplyMessageFuture<K, V> sendAndReceive(Message<?> message) {
        throw new UnsupportedOperationException();
    }

    /**
     * Send a request message and receive a reply message.
     * @param message the message to send.
     * @param replyTimeout the reply timeout; if null, the default will be used.
     * @return a RequestReplyMessageFuture.
     * @since 2.7
     */
    default StatefunRequestReplyMessageFuture<K, V> sendAndReceive(Message<?> message, @Nullable Duration replyTimeout) {
        throw new UnsupportedOperationException();
    }

    /**
     * Send a request message and receive a reply message.
     * @param message the message to send.
     * @param returnType a hint to the message converter for the reply payload type.
     * @param <P> the reply payload type.
     * @return a RequestReplyMessageFuture.
     * @since 2.7
     */
    default <P> StatefunRequestReplyTypedMessageFuture<K, V, P> sendAndReceive(Message<?> message,
                                                                       ParameterizedTypeReference<P> returnType) {

        throw new UnsupportedOperationException();
    }

    /**
     * Send a request message and receive a reply message.
     * @param message the message to send.
     * @param replyTimeout the reply timeout; if null, the default will be used.
     * @param returnType a hint to the message converter for the reply payload type.
     * @param <P> the reply payload type.
     * @return a RequestReplyMessageFuture.
     * @since 2.7
     */
    default <P> StatefunRequestReplyTypedMessageFuture<K, V, P> sendAndReceive(Message<?> message, Duration replyTimeout,
                                                                       ParameterizedTypeReference<P> returnType) {

        throw new UnsupportedOperationException();
    }

    /**
     * Send a request and receive a reply with the default timeout.
     * @param record the record to send.
     * @return a RequestReplyFuture.
     */
    StatefunRequestReplyFuture<K, V, R> sendAndReceive(ProducerRecord<K, V> record);

    /**
     * Send a request and receive a reply.
     * @param record the record to send.
     * @param replyTimeout the reply timeout; if null, the default will be used.
     * @return a RequestReplyFuture.
     * @since 2.3
     */
    StatefunRequestReplyFuture<K, V, R> sendAndReceive(ProducerRecord<K, V> record, Duration replyTimeout);

}
