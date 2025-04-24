FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar -x test --no-daemon
# TODO -x test 삭제

FROM eclipse-temurin:17
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]