FROM node:20-alpine AS frontend-builder
WORKDIR /workspace/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/index.html ./index.html
COPY frontend/vite.config.js ./vite.config.js
COPY frontend/src ./src
RUN npm run build


FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
WORKDIR /workspace

COPY backend/pom.xml ./backend/pom.xml
RUN mvn -f ./backend/pom.xml -DskipTests dependency:go-offline

COPY backend/src ./backend/src
COPY --from=frontend-builder /workspace/frontend/dist ./backend/src/main/resources/META-INF/resources

RUN mvn -f ./backend/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

COPY --from=backend-builder /workspace/backend/target/quarkus-app/lib/ ./lib/
COPY --from=backend-builder /workspace/backend/target/quarkus-app/*.jar ./
COPY --from=backend-builder /workspace/backend/target/quarkus-app/app/ ./app/
COPY --from=backend-builder /workspace/backend/target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
