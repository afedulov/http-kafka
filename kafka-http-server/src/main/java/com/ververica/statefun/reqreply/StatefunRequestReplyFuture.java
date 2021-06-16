package com.ververica.statefun.reqreply;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Copied from {@link RequestReplyFuture}.
 */
public class StatefunRequestReplyFuture<K, V, R> extends SettableListenableFuture<ConsumerRecord<K, R>> {

    private volatile ListenableFuture<SendResult<K, V>> sendFuture;

    protected void setSendFuture(ListenableFuture<SendResult<K, V>> sendFuture) {
        this.sendFuture = sendFuture;
    }

    public ListenableFuture<SendResult<K, V>> getSendFuture() {
        return this.sendFuture;
    }

}
