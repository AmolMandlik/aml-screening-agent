# Use Eclipse Adoptium Temurin JRE 21
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/aml-screening-agent-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Set labels
LABEL authors="amolmandlik"
LABEL description="AML Screening Agent - Proof of Concept"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
