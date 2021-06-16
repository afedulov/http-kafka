package com.ververica.statefun.reqreply;

import org.springframework.kafka.requestreply.RequestReplyMessageFuture;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Copied and modified from {@link RequestReplyMessageFuture}.
 */
public class StatefunRequestReplyMessageFuture<K, V> extends SettableListenableFuture<Message<?>> {

    private final ListenableFuture<SendResult<K, V>> sendFuture;

    public StatefunRequestReplyMessageFuture(ListenableFuture<SendResult<K, V>> sendFuture) {
        this.sendFuture = sendFuture;
    }

    /**
     * Return the send future.
     * @return the send future.
     */
    public ListenableFuture<SendResult<K, V>> getSendFuture() {
        return this.sendFuture;
    }

}
