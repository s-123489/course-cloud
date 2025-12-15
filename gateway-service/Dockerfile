FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/gateway-service.jar app.jar
EXPOSE 8090
# 限制JVM内存使用
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]
