#!/bin/bash

wait_for() {
  local host=$1
  local port=$2
  echo "Waiting for $host:$port..."
  while ! nc -z $host $port; do
    sleep 1
  done
  echo "$host:$port is available!"
}

wait_for mysql 3306
wait_for redis 6379

exec "$@"
