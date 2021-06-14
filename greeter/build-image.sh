#!/usr/bin/env bash

IMAGE=${IMAGE:-"austince/statefun-greeter:0.0.1"}

docker build --cache-from="$IMAGE" . -t "$IMAGE"
