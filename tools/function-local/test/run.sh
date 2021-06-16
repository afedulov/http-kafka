#!/bin/bash

source $(dirname "$0")/utils.sh

key="123"
#json=$(cat <<JSON
#  {"kind":"Invocation","apiVersion":"reqreply.statefun.ververica.com/v1alpha1","metadata":{"correlationId":"987654321"},"request":{"key":"key1","ingressTopic":"ingress","egressTopic":"egress","value":"request-payload"},"response":{"key":"key2","value":"response-payload","responderId":"responderId"}}
#JSON
#)
json=$(cat <<JSON
  {"kind":"Invocation","apiVersion":"reqreply.statefun.ververica.com/v1alpha1","metadata":{"correlationId":"987654321"},"request":{"key":"key1","ingressTopic":"ingress","egressTopic":"egress","value":"request-payload"}}
JSON
)
ingress_topic="invoke"
kafka="localhost:9092"
send_to_kafka $key $json $ingress_topic $kafka