# 1. Explicit base image version (avoids unpredictable :latest tag)
FROM eclipse-temurin:25-jre-resolute

# 2. Define explicit environment parameters and working directory
ENV PORT=8080
WORKDIR /app

# 3. Use COPY instead of ADD to prevent unintended archive extraction
COPY target/*.jar app.jar

# 4. Least-Privilege Execution: Run as non-root user for container security
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# 5. Exec-form entrypoint definition
CMD ["java", "-jar", "app.jar"]