FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY backend/jtcsm-api/target/*.jar api.jar
COPY backend/jtcsm-admin/target/*.jar admin.jar
EXPOSE 8081 8080
CMD ["sh", "-c", "java -jar api.jar & java -jar admin.jar & wait"]