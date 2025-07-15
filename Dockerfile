# Étape 1 : builder le JAR avec Maven
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : image runtime légère
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/digital-tmp-keycloak-config-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
