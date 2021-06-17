from typing import List
import json
import pprint

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
  
class Invocation(object):
    def __init__(self, metadata: Metadata, request: Request, response: Response):
        self.metadata = metadata
        self.request = request
        self.response = response
  

metadata = Metadata(correlationId="987654321")
request = Request(key="key1", ingress_topic="ingress", egress_topic="egress", value="request-payload")
response = Response(key="key2", value="response-payload", responderId="responderId")
invocation = Invocation(metadata=metadata, request=request, response=response)
  
# Serialization
json_data = json.dumps(invocation, default=lambda o: del_none(o.__dict__), indent=4)
print(json_data)
  
pp = pprint.PrettyPrinter(indent=4)

# Deserialization
des_dict = json.loads(json_data)

decoded_invocation = Invocation(metadata=des_dict.get('metadata',{}),
                                request=des_dict.get('request',{}), 
                                response=des_dict.get('response',{}))

pp.pprint(decoded_invocation.__dict__)
print(decoded_invocation.__dict__)


import base64
r = str(base64.b64encode(str(10).encode("utf-8")), "utf-8")
print(r)