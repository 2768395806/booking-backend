# ===== booking-backend 多阶段构建 =====
# 阶段一：Maven 编译（云端拉取源码后直接产出 jar）
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 优先拷贝 pom 以利用构建缓存
COPY pom.xml .
COPY .mvn ./.mvn
COPY mvnw .
RUN mvn -B -q -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -B -q -DskipTests package

# 阶段二：运行环境
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
# SQLite 与上传目录（全新文件系统下需预先创建，否则 SQLite 无法打开 data/booking.db）
RUN mkdir -p /app/data /app/uploads
COPY --from=build /build/target/booking-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8993
ENTRYPOINT ["java", "-jar", "app.jar"]
