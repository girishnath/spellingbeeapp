# Use Eclipse Temurin JDK 21 for building
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Use smaller JRE for runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built WAR file
COPY --from=build /app/target/*.war app.war

# Expose port 8080 (Cloud Run default)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.war"]
