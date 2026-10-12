# ---- Build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/manush.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
