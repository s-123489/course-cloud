FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/user-service.jar app.jar
EXPOSE 8081
# 限制JVM内存使用
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]
