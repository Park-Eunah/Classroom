FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/classroom-rental-1.0.0.jar /app/app.jar
ENV PORT=10000
EXPOSE 10000
CMD ["java", "-jar", "/app/app.jar"]
