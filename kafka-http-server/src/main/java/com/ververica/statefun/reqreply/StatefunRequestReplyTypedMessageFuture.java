package com.ververica.statefun.reqreply;

import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StatefunRequestReplyTypedMessageFuture<K, V, P> extends StatefunRequestReplyMessageFuture<K, V> {
    public StatefunRequestReplyTypedMessageFuture(ListenableFuture<SendResult<K, V>> sendFuture) {
        super(sendFuture);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public Message<P> get() throws InterruptedException, ExecutionException {
        return (Message<P>) super.get();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public Message<P> get(long timeout, @NotNull TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {

        return (Message<P>) super.get(timeout, unit);
    }


}
