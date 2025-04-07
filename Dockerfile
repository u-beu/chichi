FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar -x test --no-daemon
# 테스트 제외

FROM eclipse-temurin:17

RUN apt-get update && apt-get install -y netcat-openbsd

COPY --from=build /app/build/libs/app.jar app.jar

COPY wait-for-services.sh /wait-for-services.sh
RUN chmod +x /wait-for-services.sh

EXPOSE 8080
ENTRYPOINT ["/wait-for-services.sh", "java", "-jar", "app.jar"]