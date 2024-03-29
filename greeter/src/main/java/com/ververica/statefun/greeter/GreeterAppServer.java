/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.statefun.greeter;

import com.ververica.statefun.greeter.undertow.UndertowHttpHandler;
import io.undertow.Undertow;
import org.apache.flink.statefun.sdk.java.StatefulFunctions;
import org.apache.flink.statefun.sdk.java.handler.RequestReplyHandler;
import java.util.Optional;

/**
 * Entry point to start an {@link Undertow} web server that exposes the functions that build up our
 * User Greeter application and {@link GreetingsFn}.
 *
 * <p>Here we are using the {@link Undertow} web server just to show a very simple demonstration.
 * You may choose any web server that can handle HTTP request and responses, for example, Spring,
 * Micronaut, or even deploy your functions on popular FaaS platforms, like AWS Lambda.
 */
public final class GreeterAppServer {

  public static void main(String[] args) {
    final StatefulFunctions functions = new StatefulFunctions();
    functions.withStatefulFunction(GreetingsFn.SPEC);

    final RequestReplyHandler requestReplyHandler = functions.requestReplyHandler();

    var port = Optional.ofNullable(System.getenv("PORT"))
        .map(Integer::parseInt)
        .orElse(1108);

    final Undertow httpServer =
        Undertow.builder()
            .addHttpListener(port, "0.0.0.0")
            .setHandler(new UndertowHttpHandler(requestReplyHandler))
            .build();
    httpServer.start();
  }
}
