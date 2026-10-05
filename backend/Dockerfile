# 后端镜像：多阶段构建，无需本地 Maven 环境
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JDBC_URL=jdbc:postgresql://db:5432/market_fair \
    JDBC_USER=market \
    JDBC_PASSWORD=market \
    UPLOAD_DIR=/app/data/uploads
COPY --from=build /build/target/market-fair.jar app.jar
RUN mkdir -p /app/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
