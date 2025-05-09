#!/bin/bash

source .env
echo "현재 ACTIVE_COLOR=$ACTIVE_COLOR"
if [ "$ACTIVE_COLOR" = "blue" ]; then
    NEW_COLOR=green
    OLD_COLOR=blue
    NEW_PORT=8082
else
    NEW_COLOR=blue
    OLD_COLOR=green
    NEW_PORT=8081
fi

echo "app-$NEW_COLOR 실행"
sudo docker compose up -d app-$NEW_COLOR

echo "대기"
sleep 5

echo "ACTIVE_COLOR=$NEW_COLOR 로 변경 및 nginx 재로드"
sed -i "s/^ACTIVE_COLOR=.*/ACTIVE_COLOR=$NEW_COLOR/" .env
sudo docker exec nginx-container /bin/sh -c "cp /etc/nginx/conf.d/nginx.$NEW_COLOR.conf /etc/nginx/nginx.conf"
sudo docker exec nginx-container /bin/sh -c "nginx -t && nginx -s reload"

echo "app-$OLD_COLOR 정지 및 삭제"
sudo docker compose stop app-$OLD_COLOR
sudo docker compose rm -f app-$OLD_COLOR