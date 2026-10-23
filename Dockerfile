FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 smartlog
COPY --from=build /app/target/smartlog-api-0.0.1-SNAPSHOT.jar app.jar
USER smartlog
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
