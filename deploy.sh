#!/bin/bash

source .env
if [ "$ACTIVE_COLOR" = "blue" ]; then
    NEW_COLOR=green
    OLD_COLOR=blue
    NEW_PORT=8082
else
    NEW_COLOR=blue
    OLD_COLOR=green
    NEW_PORT=8081
fi

echo "(app-$NEW_COLOR) 실행"
docker compose up -d app-$NEW_COLOR

echo "대기"
sleep 5

echo "ACTIVE_COLOR=$NEW_COLOR" > .env
docker compose restart nginx

echo "(app-$OLD_COLOR) 정지 및 삭제"
docker compose stop app-$OLD_COLOR
docker compose rm -f app-$OLD_COLOR