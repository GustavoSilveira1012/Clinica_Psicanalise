FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /workspace/target/psicogest-*.jar /app/psicogest.jar

# pgJDBC verify-full reads ${user.home}/.postgresql/root.crt by default.
# This public CA certificate is the one linked by the approved Supabase project.
# Keep the image fail-closed if the database certificate cannot be verified.
RUN mkdir -p /home/psicogest/.postgresql
COPY certs/supabase-prod-ca-2021.crt /home/psicogest/.postgresql/root.crt
RUN chown -R 10001:10001 /home/psicogest

ENV HOME=/home/psicogest
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/urandom -Duser.home=/home/psicogest"
EXPOSE 8080
USER 10001
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/psicogest.jar"]
