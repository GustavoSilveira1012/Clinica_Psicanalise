FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /workspace/target/psicogest-*.jar /app/psicogest.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/urandom"
EXPOSE 8080
USER 10001
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/psicogest.jar"]
