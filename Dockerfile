# ===== booking-backend 容器化 =====
# 构建：先在本机执行 mvnw clean package -DskipTests，再 docker build
# 数据持久化：容器工作目录为 /app，SQLite 数据库位于 /app/data/booking.db
# 云托管/运行时请将持久化存储挂载到 /app/data（以及 /app/uploads，若有上传目录）
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY target/booking-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8993
ENTRYPOINT ["java", "-jar", "app.jar"]
