################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

import uuid
from statefun import *
import logging
import statefun
import json

from remote_module_verification_pb2 import Invoke, InvokeResult

InvokeType = make_protobuf_type(namespace="statefun.e2e", cls=Invoke)
InvokeResultType = make_protobuf_type(namespace="statefun.e2e", cls=InvokeResult)

functions = StatefulFunctions()

class Metadata(object):
    def __init__(self, correlationId: str):
        self.correlationId = correlationId

class Request(object):
    def __init__(self, key: str, ingress_topic: str, egress_topic: str, value: str):
        self.key = key
        self.ingressTopic = ingress_topic
        self.egressTopic = egress_topic
        self.value = value

class Response(object):
    def __init__(self, key: str, value: str, responderId: str):
        self.key = key
        self.value = value
        self.responderId = responderId
  

def del_none(d):
    """
    Delete keys with the value ``None`` in a dictionary, recursively.

    This alters the input so you may wish to ``copy`` the dict first.
    """
    # For Python 3, write `list(d.items())`; `d.items()` won’t work
    # For Python 2, write `d.items()`; `d.iteritems()` won’t work
    for key, value in list(d.items()):
        if value is None:
            del d[key]
        elif isinstance(value, dict):
            del_none(value)
    return d  # For convenience


def serialize(invocation):
    return json.dumps(invocation, default=lambda o: del_none(o.__dict__), indent=4).encode()

def deserialize(data):
    des_dict = json.loads(data)
    return Invocation(
                    kind=des_dict.get('kind',{}),
                    apiVersion=des_dict.get('apiVersion',{}),
                    metadata=des_dict.get('metadata',{}),
                    request=des_dict.get('request',{}), 
                    response=des_dict.get('response',{}))

class Invocation(object):
    TYPE: statefun.Type = statefun.simple_type(
        typename='com.ververica.types/invocation',
        serialize_fn=lambda invocation: serialize(invocation),
        deserialize_fn=lambda data: deserialize(data))


    def __init__(self,  kind: str, apiVersion: str, metadata: Metadata, request: Request, response: Response):
        self.kind = kind
        self.apiVersion = apiVersion
        self.metadata = metadata
        self.request = request
        self.response = response


@functions.bind(
    typename="org.apache.flink.statefun.e2e.remote/counter",
    specs=[ValueSpec(name='invoke_count', type=IntType)])
def counter(context, message):
    logging.warning(">> MESSAGE RECEIVED:")
    logging.warning(message)

    invocation = message.as_type(Invocation.TYPE)
    logging.warning(invocation.__dict__)

    """
    Keeps count of the number of invocations, and forwards that count
    to be sent to the Kafka egress. We do the extra forwarding instead
    of directly sending to Kafka, so that we cover inter-function
    messaging in our E2E test.
    """
    n = context.storage.invoke_count or 0
    n += 1
    context.storage.invoke_count = n

    logging.warning(invocation.request)
    
    response = Response(key="somekey", value=n, responderId=context.address.namespace + "/" +context.address.name)
    invocation.response = response

    egress_message = kafka_egress_message(
        typename="org.apache.flink.statefun.e2e.remote/invoke-results",
        topic="invoke-results",
        key=response.key,
        value=invocation,
        value_type=Invocation.TYPE)
        # value="correlation_id=123")
        # value_type="io.statefun.types/string")
    context.send_egress(egress_message)

    # context.send(
    #     message_builder(target_typename="org.apache.flink.statefun.e2e.remote/forward-function",
    #                     # use random keys to simulate both local handovers and
    #                     # cross-partition messaging via the feedback loop
    #                     target_id=uuid.uuid4().hex,
    #                     value=response,
    #                     value_type=InvokeResultType))


@functions.bind("org.apache.flink.statefun.e2e.remote/forward-function")
def forward_to_egress(context, message):
    print(">>> MESSAGE RECEIVED IN FORWARD!!!", flush=True)
    """
    Simply forwards the results to the Kafka egress.
    """
    invoke_result = message.as_type(InvokeResultType)

    egress_message = kafka_egress_message(
        typename="org.apache.flink.statefun.e2e.remote/invoke-results",
        topic="invoke-results",
        key=invoke_result.id,
        # value=invoke_result,
        value="correlation_id=123")
        # value_type="io.statefun.types/string")
    context.send_egress(egress_message)


handler = RequestReplyHandler(functions)

#
# Serve the endpoint
#

from flask import request
from flask import make_response
from flask import Flask

app = Flask(__name__)




@app.route('/service', methods=['POST'])
def handle():
    response_data = handler.handle_sync(request.data)
    response = make_response(response_data)
    response.headers.set('Content-Type', 'application/octet-stream')
    return response


if __name__ == "__main__":
    logging.warning(">> STARTING REMOTE FUNCTION!")
    print(">> STARTING REMOTE FUNCTION!", flush=True)
    app.run(host='0.0.0.0', port=8000)


  