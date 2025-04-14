FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17
COPY --from=build /app/build/libs/app.jar app.jar

RUN apt-get update && apt-get install -y netcat-openbsd
COPY wait-for-services.sh /wait-for-services.sh

RUN chmod +x /wait-for-services.sh
EXPOSE 8080
ENTRYPOINT ["/wait-for-services.sh", "java", "-jar", "app.jar"]